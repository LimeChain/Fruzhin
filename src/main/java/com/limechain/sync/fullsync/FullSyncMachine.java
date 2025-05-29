package com.limechain.sync.fullsync;

import com.google.protobuf.ByteString;
import com.limechain.config.HostConfig;
import com.limechain.consensus.babe.coordinator.SlotChangeListener;
import com.limechain.consensus.babe.coordinator.SlotCoordinator;
import com.limechain.consensus.beefy.BeefyService;
import com.limechain.consensus.grandpa.GrandpaService;
import com.limechain.exception.global.RuntimeCodeException;
import com.limechain.exception.storage.BlockNodeNotFoundException;
import com.limechain.exception.sync.BlockExecutionException;
import com.limechain.network.NetworkService;
import com.limechain.network.PeerMessageCoordinator;
import com.limechain.network.PeerRequester;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.network.protocol.sync.BlockRequestField;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.network.protocol.warp.DigestHelper;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.CodeChangeChecker;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.runtime.RuntimeStorageKey;
import com.limechain.runtime.version.StateVersion;
import com.limechain.state.AbstractState;
import com.limechain.state.StateManager;
import com.limechain.storage.block.BlockHandler;
import com.limechain.storage.block.state.BlockState;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.sync.SyncMode;
import com.limechain.sync.fullsync.inherents.InherentData;
import com.limechain.sync.state.SyncState;
import com.limechain.trie.DiskTrieAccessor;
import com.limechain.trie.TrieAccessor;
import com.limechain.trie.TrieAccessorStorage;
import com.limechain.trie.TrieStructureFactory;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.PairReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FullSyncMachine is responsible for executing full synchronization of blocks.
 * It communicates with the network to fetch blocks, execute them, and update the trie structure accordingly.
 */
@Getter
@Log
public class FullSyncMachine {

    private final HostConfig hostConfig;
    private final NetworkService networkService;
    private final StateManager stateManager;
    private final PeerRequester requester;
    private final PeerMessageCoordinator messageCoordinator;
    private final BlockHandler blockHandler;
    private final TrieAccessorStorage trieAccessorStorage;
    private final CodeChangeChecker codeChangeChecker;
    private final List<SlotChangeListener> slotChangeListeners;
    private final TrieStorage trieStorage = AppBean.getBean(TrieStorage.class);
    private final RuntimeBuilder runtimeBuilder = AppBean.getBean(RuntimeBuilder.class);
    private final SlotCoordinator slotCoordinator = AppBean.getBean(SlotCoordinator.class);
    private final GrandpaService grandpaService = AppBean.getBean(GrandpaService.class);
    private final BeefyService beefyService = AppBean.getBean(BeefyService.class);
    private Runtime runtime = null;

    public FullSyncMachine(NetworkService networkService,
                           StateManager stateManager,
                           PeerRequester requester,
                           PeerMessageCoordinator messageCoordinator,
                           BlockHandler blockHandler,
                           TrieAccessorStorage trieAccessorStorage,
                           CodeChangeChecker codeChangeChecker,
                           List<SlotChangeListener> slotChangeListeners,
                           HostConfig hostConfig) {
        this.networkService = networkService;
        this.stateManager = stateManager;
        this.requester = requester;
        this.messageCoordinator = messageCoordinator;
        this.blockHandler = blockHandler;
        this.trieAccessorStorage = trieAccessorStorage;
        this.codeChangeChecker = codeChangeChecker;
        this.slotChangeListeners = slotChangeListeners;
        this.hostConfig = hostConfig;
    }

    public void start() {
        SyncState syncState = stateManager.getSyncState();
        Hash256 stateRoot = syncState.getStateRoot();
        Hash256 lastFinalizedBlockHash = syncState.getLastFinalizedBlockHash();

        if (!trieStorage.merkleValueExists(stateRoot)) {
            //TODO Sync improvements: This does not work on polkadot chain.
            loadStateAtBlockFromPeer(lastFinalizedBlockHash);
        }

        DiskTrieAccessor trieAccessor = trieAccessorStorage.init(
                lastFinalizedBlockHash, stateRoot);

        var optCode = trieAccessor.findStorageValue(RuntimeStorageKey.CODE.getNibbles());
        if (optCode.isEmpty()) {
            throw new RuntimeCodeException("Runtime code should not be empty");
        }

        runtime = runtimeBuilder.buildRuntime(optCode.get(), trieAccessor);
        StateVersion runtimeStateVersion = runtime.getCachedVersion().getStateVersion();
        trieAccessor.setCurrentStateVersion(runtimeStateVersion);

        codeChangeChecker.initStoredCode(optCode.get());

        byte[] calculatedMerkleRoot = trieAccessor.getMerkleRoot(runtimeStateVersion);
        if (!stateRoot.equals(new Hash256(calculatedMerkleRoot))) {
            log.info("State root is not equal to the one in the trie, cannot start full sync");
            return;
        }

        stateManager.getBlockState().storeRuntime(lastFinalizedBlockHash, runtime);

        if (networkService.isStarted()) {

            int startNumber = syncState.getLastFinalizedBlockNumber()
                    .add(BigInteger.ONE)
                    .intValueExact();

            int blocksToFetch = 100;
            List<Block> receivedBlocks = requester.requestBlocks(BlockRequestField.ALL, startNumber, blocksToFetch).join();

            while (!receivedBlocks.isEmpty()) {
                executeBlocks(receivedBlocks, trieAccessor);

                log.info(String.format("Executed blocks from #%d to #%d",
                        receivedBlocks.getFirst().getHeader().getBlockNumber(),
                        receivedBlocks.getLast().getHeader().getBlockNumber()));

                startNumber += receivedBlocks.size();
                receivedBlocks = requester.requestBlocks(BlockRequestField.ALL, startNumber, blocksToFetch).join();
            }
        }

        BlockState blockState = stateManager.getBlockState();
        var lastJustificationEntry = blockState.getJustifications().lastEntry();
        if (lastJustificationEntry != null) {
            grandpaService.finalizeJustification(lastJustificationEntry.getValue());
        }

        finishFullSync();
    }

    private void finishFullSync() {
        stateManager.getEpochState().populateDataFromRuntime(runtime);
        stateManager.getGrandpaSetState().populateDataFromRuntime(runtime);
        stateManager.getBeefyState().populateDataFromRuntime(runtime);

        if (NodeRole.AUTHORING.equals(hostConfig.getNodeRole())) {
            slotCoordinator.start(slotChangeListeners);
        }
        grandpaService.start();
        beefyService.start();

        AbstractState.setSyncMode(SyncMode.HEAD);
    }

    private TrieStructure<NodeData> loadStateAtBlockFromPeer(Hash256 lastFinalizedBlockHash) {
        log.info("Loading state at block from peer");
        Map<ByteString, ByteString> kvps = makeStateRequest(lastFinalizedBlockHash);

        TrieStructure<NodeData> trieStructure = TrieStructureFactory.buildFromKVPs(kvps);
        trieStorage.insertTrieStorage(trieStructure);
        log.info("State at block loaded from peer");

        kvps.clear();
        return trieStructure;
    }

    private Map<ByteString, ByteString> makeStateRequest(Hash256 lastFinalizedBlockHash) {
        Map<ByteString, ByteString> kvps = new HashMap<>();

        ByteString start = ByteString.EMPTY;

        while (true) {
            final SyncMessage.StateResponse response;
            try {
                response = requester.requestState(lastFinalizedBlockHash.toString(), start).join();
            } catch (Exception ex) {
                if (!this.networkService.updateCurrentSelectedPeerWithNextBootnode()) {
                    this.networkService.updateCurrentSelectedPeer();
                }
                continue;
            }

            for (SyncMessage.KeyValueStateEntry keyValueStateEntry : response.getEntriesList()) {
                for (SyncMessage.StateEntry stateEntry : keyValueStateEntry.getEntriesList()) {
                    kvps.put(stateEntry.getKey(), stateEntry.getValue());
                }
            }

            SyncMessage.KeyValueStateEntry lastEntry = response.getEntriesList().getLast();
            if (!lastEntry.getComplete()) {
                start = lastEntry.getEntriesList().getLast().getKey();
            } else {
                break;
            }
        }

        return kvps;
    }

    /**
     * Executes blocks received from the network.
     *
     * @param receivedBlockDatas A list of BlockData to execute.
     */
    private void executeBlocks(List<Block> receivedBlockDatas, TrieAccessor trieAccessor) {
        BlockState blockState = stateManager.getBlockState();
        for (Block block : receivedBlockDatas) {
            log.info(String.format("Block #%d to be executed", block.getHeader().getBlockNumber()));

            // Check the block for valid inherents
            // NOTE: This is only relevant for block production.
            //  We will need this functionality in near future,
            //  but we don't need it when importing blocks for the full sync.
            boolean goodToExecute = this.checkInherents(block);

            log.fine("Block is good to execute: " + goodToExecute);

            if (!goodToExecute) {
                log.fine("Block not executed");
                throw new BlockExecutionException();
            }

            // Actually execute the block and persist changes
            runtime.executeBlock(block.getHeader(), block);
            log.fine("Block executed successfully");

            try {
                blockHandler.addBlockToTree(block, Instant.now());
            } catch (BlockNodeNotFoundException ex) {
                log.fine(String.format("Executing block #%d which has no parent in block state",
                        block.getHeader().getBlockNumber()));
            }

            BlockHeader blockHeader = block.getHeader();
            codeChangeChecker.checkRuntimeCodeChange(blockHeader).ifPresent(r -> {
                log.fine(String.format(
                        "Runtime update deteched for block: %s", block.getHeader().getBlockNumber()));
                runtime = r;
            });

            finalizeDuringSync(blockHeader);
        }
    }

    private void syncGrandpaJustification() {
        WarpSyncFragment lastFinalized;

        try {
            var fragments = requester.makeWarpSyncRequest(
                            stateManager.getBlockState().getLastFinalized().toString())
                    .join().getFragments();

            int length = fragments.length;
            log.info("Warp sync request length: " + length);
            lastFinalized = fragments[length - 1];
        } catch (Exception ex) {
            log.warning(String.format("Warp sync request failed. Cause: %s", ex.getMessage()));
            return;
        }

        stateManager.getBlockState().getJustifications().put(
                lastFinalized.getHeader().getHash(),
                lastFinalized.getJustification());

        log.info("Added finalized state: " + lastFinalized.getHeader().getHash() + ", " + lastFinalized.getHeader().getBlockNumber());
    }

    /**
     * Finalizes blocks during the full sync process. A block is finalized under two conditions:<br>
     * - There are no received grandpa justifications. In a working node this would moslty happen if we are not at the
     * latest grandpa set.<br>
     * - The block number is lower than the latest received grandpa justification.
     *
     * @param blockHeader the block header that could be finalized.
     */
    private void finalizeDuringSync(BlockHeader blockHeader) {

        BlockState blockState = stateManager.getBlockState();

        boolean lowerThanJustif = false;

        var latestJustification = blockState.getJustifications().lastEntry();
        if (latestJustification != null) {
            lowerThanJustif = latestJustification.getValue().getTargetBlock()
                    .compareTo(blockHeader.getBlockNumber()) > 0;
        }

        if (blockState.getJustifications().isEmpty() || lowerThanJustif) {
            blockState.finalizeBlock(blockHeader, null, BigInteger.ZERO);
            stateManager.getSyncState().finalizeBlock(blockHeader);

            stateManager.getGrandpaSetState()
                    .applyAuthoritySetChange(
                            blockHeader.getHash(),
                            blockHeader.getBlockNumber()
                    );

            DigestHelper.getBeefyConsensusMessages(blockHeader.getDigest())
                    .forEach(cm -> stateManager.getBeefyState().handleBeefyConsensusMessage(
                            cm, blockHeader.getBlockNumber())
                    );

            log.fine(String.format("finalizeIfNeeded: Finalizing block #%d (%s)",
                    blockHeader.getBlockNumber(),
                    blockHeader.getPrintableHash()));
        }
    }

    private boolean checkInherents(Block block) {
        // Call BlockBuilder_check_inherents to check the inherents of the block
        InherentData inherents = new InherentData(System.currentTimeMillis());
        byte[] checkInherentsOutput = runtime.checkInherents(null, block, inherents);

        // Check if the block is good to execute based on the output of BlockBuilder_check_inherents
        return isBlockGoodToExecute(checkInherentsOutput);
    }

    /**
     * Checks whether a block is good to execute based on the output of BlockBuilder_check_inherents.
     *
     * @param checkInherentsOutput The output of BlockBuilder_check_inherents.
     * @return True if the block is good to execute, false otherwise.
     */
    private static boolean isBlockGoodToExecute(byte[] checkInherentsOutput) {
        var data = ScaleUtils.Decode.decode(
                ArrayUtils.subarray(checkInherentsOutput, 2, checkInherentsOutput.length),
                new ListReader<>(
                        new PairReader<>(
                                scr -> new String(scr.readByteArray(8)),
                                scr -> new String(scr.readByteArray())
                        )
                )
        );

        boolean goodToExecute;

        if (data.size() > 1) {
            goodToExecute = false;
        } else if (data.size() == 1) {
            //If the inherent is babeslot or auraslot, then it's an expected issue and we can proceed
            goodToExecute = data.get(0).getValue0().equals("babeslot") || data.get(0).getValue0().equals("auraslot");
        } else {
            goodToExecute = true;
        }
        return goodToExecute;
    }
}
