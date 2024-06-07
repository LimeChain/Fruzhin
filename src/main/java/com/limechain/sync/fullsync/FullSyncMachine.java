package com.limechain.sync.fullsync;

import com.limechain.exception.sync.BlockExecutionException;
import com.limechain.network.Network;
import com.limechain.network.protocol.sync.BlockRequestDto;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockBody;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.Extrinsics;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.block.BlockState;
import com.limechain.storage.block.SyncState;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.sync.fullsync.inherents.InherentData;
import com.limechain.trie.BlockTrieAccessor;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.PairReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * FullSyncMachine is responsible for executing full synchronization of blocks.
 * It communicates with the network to fetch blocks, execute them, and update the trie structure accordingly.
 */
@Getter
@Log
public class FullSyncMachine {
    private final Network networkService;
    private final SyncState syncState;
    private final BlockState blockState = BlockState.getInstance();
    private final TrieStorage trieStorage = AppBean.getBean(TrieStorage.class);
    private Runtime runtime = null;

    public FullSyncMachine(final Network networkService, final SyncState syncState) {
        this.networkService = networkService;
        this.syncState = syncState;
    }

    public void start() {
        // TODO: DIRTY INITIALIZATION FIX:
        //  this.networkService.currentSelectedPeer is null,
        //  unless explicitly set via some of the "update..." methods
        this.networkService.updateCurrentSelectedPeerWithNextBootnode();

        Hash256 lastFinelizedStateRoot = syncState.getStateRoot(); //to replace line 67
        // TODO: fetch state of the latest finalized block and start executing blocks from there
        // scrap the blockstate highest finalized header logic for now

        BlockHeader highestFinalizedHeader = blockState.getHighestFinalizedHeader();
        Hash256 stateRoot = highestFinalizedHeader.getStateRoot();

        BlockTrieAccessor blockTrieAccessor = new BlockTrieAccessor(trieStorage, stateRoot.getBytes());

        byte[] calculatedMerkleRoot = blockTrieAccessor.getMerkleRoot(StateVersion.V0);
        if (!stateRoot.equals(new Hash256(calculatedMerkleRoot))) {
            log.info("State root is not equal to the one in the trie, cannot start full sync");
            return;
        }

        runtime = buildRuntimeFromState(blockTrieAccessor);
        blockState.storeRuntime(highestFinalizedHeader.getHash(), runtime);

        int startNumber = highestFinalizedHeader
                .getBlockNumber()
                .add(BigInteger.ONE)
                .intValue();

        int blocksToFetch = 100;
        List<Block> receivedBlocks = requestBlocks(startNumber, blocksToFetch);

        while (!receivedBlocks.isEmpty()) {
            executeBlocks(receivedBlocks, blockTrieAccessor);
            log.info("Executed blocks from " + startNumber + " to " + (startNumber + blocksToFetch));
            startNumber += blocksToFetch;
            receivedBlocks = requestBlocks(startNumber, blocksToFetch);
        }
    }

    /**
     * Fetches blocks from the network.
     *
     * @param start  The block number to start fetching from.
     * @param amount The number of blocks to fetch.
     * @return A list of BlockData received from the network.
     */
    private List<Block> requestBlocks(int start, int amount) {
        try {
            final int HEADER = 0b0000_0001;
            final int BODY = 0b0000_0010;
            final int JUSTIFICATION = 0b0001_0000;
            SyncMessage.BlockResponse response = networkService.makeBlockRequest(new BlockRequestDto(
                    HEADER | BODY | JUSTIFICATION,
                    null, // no hash, number instead
                    start,
                    SyncMessage.Direction.Ascending,
                    amount
            ));

            List<SyncMessage.BlockData> blockDatas = response.getBlocksList();

            return blockDatas.stream()
                .map(FullSyncMachine::protobufDecodeBlock)
                .toList();
        } catch (Exception ex) {
            log.info("Error while fetching blocks, trying to fetch again");
            this.networkService.updateCurrentSelectedPeerWithNextBootnode();
            return requestBlocks(start, amount);
        }
    }

    // TODO: This method doesn't belong in this class. Move when appropriate.
    private static Block protobufDecodeBlock(SyncMessage.BlockData blockData) {
        // Decode the block header
        var encodedHeader = blockData.getHeader().toByteArray();
        BlockHeader blockHeader = ScaleUtils.Decode.decode(encodedHeader, new BlockHeaderReader());

        // Protobuf decode the block body
        List<Extrinsics> extrinsicsList = blockData.getBodyList().stream()
            .map(bs -> ScaleUtils.Decode.decode(bs.toByteArray(), ScaleCodecReader::readByteArray))
            .map(Extrinsics::new)
            .toList();

        BlockBody blockBody = new BlockBody(extrinsicsList);

        return new Block(blockHeader, blockBody);
    }

    /**
     * Executes blocks received from the network.
     *
     * @param receivedBlockDatas A list of BlockData to execute.
     */
    private void executeBlocks(List<Block> receivedBlockDatas, BlockTrieAccessor blockTrieAccessor) {
        for (Block block : receivedBlockDatas) {
            log.fine("Block number to be executed is " + block.getHeader().getBlockNumber());

            blockState.addBlock(block);

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

            // Persist the updates to the trie structure
            blockTrieAccessor.persistUpdates();

            BlockHeader blockHeader = block.getHeader();
            blockState.setFinalizedHash(blockHeader.getHash(), BigInteger.ZERO, BigInteger.ZERO);

            boolean blockUpdatedRuntime = Arrays.stream(blockHeader.getDigest())
                .map(HeaderDigest::getType)
                .anyMatch(type -> type.equals(DigestType.RUN_ENV_UPDATED));

            if (blockUpdatedRuntime) {
                log.info("Runtime updated, updating the runtime code");
                runtime = buildRuntimeFromState(blockTrieAccessor);
                blockState.storeRuntime(blockHeader.getHash(), runtime);
            }
        }
    }

    private static Runtime buildRuntimeFromState(BlockTrieAccessor blockTrieAccessor) {
        return blockTrieAccessor
                .find(Nibbles.fromBytes(":code".getBytes()))
                .map(wasm -> new RuntimeBuilder().buildRuntime(wasm, blockTrieAccessor))
                .orElseThrow(() -> new RuntimeException("Runtime code not found in the trie"));
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
