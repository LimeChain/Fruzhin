package com.limechain.sync.fullsync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.limechain.config.HostConfig;
import com.limechain.consensus.babe.BabeService;
import com.limechain.consensus.babe.coordinator.SlotCoordinator;
import com.limechain.consensus.beefy.BeefyService;
import com.limechain.consensus.grandpa.GrandpaService;
import com.limechain.exception.storage.BlockNodeNotFoundException;
import com.limechain.exception.sync.BlockExecutionException;
import com.limechain.network.NetworkService;
import com.limechain.network.PeerMessageCoordinator;
import com.limechain.network.PeerRequester;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.network.protocol.sync.BlockRequestField;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
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
import com.limechain.trie.TrieStructureFactory;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.PairReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;
import org.javatuples.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                           HostConfig hostConfig) {
        this.networkService = networkService;
        this.stateManager = stateManager;
        this.requester = requester;
        this.messageCoordinator = messageCoordinator;
        this.blockHandler = blockHandler;
        this.hostConfig = hostConfig;
    }

    public void start() {
        ByteString test = ByteString.fromHex("074b65e262fcd5bd9c785caf7f42e00a29f2dc2b64e354002faef2a81f1aa8bb468cfbd3f67e03452e832420d3d2a3c42cca4d2db7e7086fa0994264a6f901a533c23bfc9c9935797c466c66d061f1692200");

        SyncState syncState = stateManager.getSyncState();
        Hash256 stateRoot = syncState.getStateRoot();
        Hash256 lastFinalizedBlockHash = syncState.getLastFinalizedBlockHash();

        DiskTrieAccessor trieAccessor = new DiskTrieAccessor(trieStorage, stateRoot.getBytes());

        if (!trieStorage.merkleValueExists(stateRoot)) {
            //TODO Sync improvements: This does not work on polkadot chain.
            loadStateAtBlockFromPeer(lastFinalizedBlockHash);
        }

        runtime = runtimeBuilder.buildRuntimeFromState(trieAccessor);
        StateVersion runtimeStateVersion = runtime.getCachedVersion().getStateVersion();
        trieAccessor.setCurrentStateVersion(runtimeStateVersion);

        byte[] calculatedMerkleRoot = trieAccessor.getMerkleRoot(runtimeStateVersion);
        if (!stateRoot.equals(new Hash256(calculatedMerkleRoot))) {
            log.info("State root is not equal to the one in the trie, cannot start full sync");
            return;
        }

        stateManager.getBlockState().storeRuntime(lastFinalizedBlockHash, runtime);

        int startNumber = syncState.getLastFinalizedBlockNumber()
                .add(BigInteger.ONE)
                .intValueExact();

        messageCoordinator.handshakeBootNodes();
        messageCoordinator.handshakePeers();

        int blocksToFetch = 100;
        List<Block> receivedBlocks = requester.requestBlocks(BlockRequestField.ALL, startNumber, blocksToFetch).join();

        while (!receivedBlocks.isEmpty()) {
            executeBlocks(receivedBlocks, trieAccessor);
            log.info("Executed blocks from " + receivedBlocks.getFirst().getHeader().getBlockNumber()
                    + " to " + receivedBlocks.getLast().getHeader().getBlockNumber());
            startNumber += receivedBlocks.size();
            receivedBlocks = requester.requestBlocks(BlockRequestField.ALL, startNumber, blocksToFetch).join();
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
            slotCoordinator.start(List.of(
                    AppBean.getBean(BabeService.class)
            ));
        }
        grandpaService.start();
        beefyService.start();

        AbstractState.setSyncMode(SyncMode.HEAD);
    }

    private TrieStructure<NodeData> loadStateAtBlockFromPeer(Hash256 lastFinalizedBlockHash) {

        log.info("test");

        List<Pair<ByteString, ByteString>> test = new ArrayList<>();

        var test1 = loadKeyValuePairs();
        log.info(test1.size() + " key pairs loaded");

        for (List<String> s : test1) {
            test.add(Pair.with(ByteString.fromHex(s.get(0)), ByteString.fromHex(s.get(1))));
        }

        test1 = null;
        log.info(test.size() + " key pairs loaded");

//        Map<ByteString, ByteString> test = loadKeyValuePairs().stream()
//                .map(e -> Map.entry(ByteString.fromHex(e.get(0)), ByteString.fromHex(e.get(1))))
//                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        TrieStructure<NodeData> trieStructure = TrieStructureFactory.buildFromKVPsTest(test);
        trieStorage.insertTrieStorage(trieStructure);
        log.info("State at block loaded from peer");

//        kvps.clear();
        return trieStructure;
    }

    @SneakyThrows
    public static List<List<String>> loadKeyValuePairs() {
        ObjectMapper mapper = new ObjectMapper();

        // Read the JSON as List<List<String>>
        List<List<String>> arrayOfPairs = parseArrayFile();

        return arrayOfPairs;
    }

    public static List<List<String>> parseArrayFile() throws IOException {
        List<List<String>> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("genesis/polkadot_state_dump_first.json"))) {
            int ch;
            boolean inQuote = false;
            boolean inInnerArray = false;
            StringBuilder currentValue = new StringBuilder();
            List<String> currentPair = new ArrayList<>(2);

            while ((ch = reader.read()) != -1) {
                char c = (char) ch;

                if (c == '[' && !inQuote) {
                    if (!inInnerArray) {
                        inInnerArray = true;
                        currentPair = new ArrayList<>(2);
                    }
                } else if (c == ']' && !inQuote) {
                    if (inInnerArray) {
                        if (currentPair.size() == 2) {
                            result.add(currentPair);
                        }
                        inInnerArray = false;
                    }
                } else if (c == '\"') {
                    inQuote = !inQuote;
                    if (!inQuote) {
                        currentPair.add(currentValue.toString());
                        currentValue.setLength(0);
                    }
                } else if (inQuote) {
                    currentValue.append(c);
                }
            }
        }

        return result;
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
            log.info("Block number to be executed is " + block.getHeader().getBlockNumber());

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
            runtime.executeBlock(block);
            log.fine("Block executed successfully");

            try {
                blockHandler.addBlockToTree(block, Instant.now());
            } catch (BlockNodeNotFoundException ex) {
                log.fine("Executing block with number " + block.getHeader().getBlockNumber()
                        + " which has no parent in block state.");
            }

            BlockHeader blockHeader = block.getHeader();
            boolean blockUpdatedRuntime = Arrays.stream(blockHeader.getDigest())
                    .map(HeaderDigest::getType)
                    .anyMatch(type -> type.equals(DigestType.RUN_ENV_UPDATED));

            if (blockUpdatedRuntime) {
                log.info("Runtime updated, updating the runtime code");
                runtime = runtimeBuilder.buildRuntimeFromState(trieAccessor);
                trieAccessor.setCurrentStateVersion(runtime.getCachedVersion().getStateVersion());
                blockState.storeRuntime(blockHeader.getHash(), runtime);
            }

            finalizeDuringSync(blockHeader);
        }
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

            log.fine(String.format("finalizeIfNeeded: Finalizing block #%d with hash %s",
                    blockHeader.getBlockNumber(),
                    blockHeader.getHash()));
        }
    }

    private boolean checkInherents(Block block) {
        // Call BlockBuilder_check_inherents to check the inherents of the block
        InherentData inherents = new InherentData(System.currentTimeMillis());
        byte[] checkInherentsOutput = runtime.checkInherents(block, inherents);

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
