package com.limechain.sync.fullsync;

import com.google.protobuf.ByteString;
import com.limechain.exception.sync.BlockExecutionException;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.scale.BlockHeaderScaleWriter;
import com.limechain.network.protocol.sync.BlockRequestDto;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockBody;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.Extrinsics;
import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.block.BlockState;
import com.limechain.sync.fullsync.inherents.InherentData;
import com.limechain.sync.fullsync.inherents.scale.InherentDataWriter;
import com.limechain.trie.AccessorHolder;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.PairReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;
import java.util.List;

/**
 * FullSyncMachine is responsible for executing full synchronization of blocks.
 * It communicates with the network to fetch blocks, execute them, and update the trie structure accordingly.
 */
@Getter
@Log
public class FullSyncMachine {
    private final Network networkService;
    private final BlockState blockState = BlockState.getInstance();
    private final AccessorHolder accessorHolder = AccessorHolder.getInstance();

    public FullSyncMachine(final Network networkService) {
        this.networkService = networkService;
    }

    public void start() {
        // TODO: DIRTY INITIALIZATION FIX:
        //  this.networkService.currentSelectedPeer is null,
        //  unless explicitly set via some of the "update..." methods
        this.networkService.updateCurrentSelectedPeerWithNextBootnode();

        BlockHeader highestFinalizedHeader = blockState.getHighestFinalizedHeader();
        Hash256 stateRoot = highestFinalizedHeader.getStateRoot();
        accessorHolder.setToStateRoot(stateRoot.getBytes());

        byte[] calculatedMerkleRoot = accessorHolder.getBlockTrieAccessor().getMerkleRoot(StateVersion.V0);
        if (!stateRoot.equals(new Hash256(calculatedMerkleRoot))) {
            log.info("State root is not equal to the one in the trie, cannot start full sync");
            return;
        }

        int startNumber = highestFinalizedHeader
                .getBlockNumber()
                .add(BigInteger.ONE)
                .intValue();
        int blocksToFetch = 100;
        List<SyncMessage.BlockData> receivedBlockDatas = getBlocks(startNumber, blocksToFetch);
        while (!receivedBlockDatas.isEmpty()) {
            startNumber += blocksToFetch;
            executeBlocks(receivedBlockDatas);
            receivedBlockDatas = getBlocks(startNumber, blocksToFetch);
        }
    }

    /**
     * Fetches blocks from the network.
     *
     * @param start  The block number to start fetching from.
     * @param amount The number of blocks to fetch.
     * @return A list of BlockData received from the network.
     */
    private List<SyncMessage.BlockData> getBlocks(int start, int amount) {
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

            // Get the block from the list of responses:
            return response.getBlocksList();
        } catch (Exception ex) {
            log.info("Error while fetching blocks, trying to fetch again");
            this.networkService.updateCurrentSelectedPeerWithNextBootnode();
            return getBlocks(start, amount);
        }
    }

    /**
     * Executes blocks received from the network.
     *
     * @param receivedBlockDatas A list of BlockData to execute.
     */
    private void executeBlocks(List<SyncMessage.BlockData> receivedBlockDatas) {
        byte[] runtimeCode = Objects.requireNonNull(AppBean.getBean(GenesisBlockHash.class)
                        .getGenesisTrie()
                        .node(Nibbles.fromBytes(":code".getBytes()))
                        .asNodeHandle()
                        .getUserData())
                .getValue();

        Runtime runtime = new RuntimeBuilder()
                .buildRuntime(runtimeCode);
        for (SyncMessage.BlockData blockData : receivedBlockDatas) {
            // Protobuf decode the block header
            var encodedHeader = blockData.getHeader().toByteArray();
            BlockHeader blockHeader = ScaleUtils.Decode.decode(encodedHeader, new BlockHeaderReader());
            BigInteger blockNumber = blockHeader.getBlockNumber();
            log.info("Block number to be executed is " + blockNumber);
            byte[] encodedUnsealedHeader =
                    ScaleUtils.Encode.encode(BlockHeaderScaleWriter.getInstance()::writeUnsealed, blockHeader);

            // Protobuf decode the block body and scale encode it
            var extrinsincs = blockData.getBodyList();
            var encodedBody = ScaleUtils.Encode.encodeAsList(
                    ScaleCodecWriter::writeByteArray,
                    () -> extrinsincs.stream().map(ByteString::toByteArray).iterator()
            );

            List<Extrinsics> extrinsicsList =
                    extrinsincs.stream().map(ByteString::toByteArray).map(Extrinsics::new).toList();
            blockState.addBlock(new Block(blockHeader, new BlockBody(extrinsicsList)));

            // Construct the parameter for executing the block
            byte[] executeBlockParameter = ArrayUtils.addAll(encodedUnsealedHeader, encodedBody);

            // Call BlockBuilder_check_inherents to check the inherents of the block
            var args = getCheckInherentsParameter(executeBlockParameter);
            byte[] checkInherentsOutput = runtime.call("BlockBuilder_check_inherents", args);

            // Check if the block is good to execute based on the output of BlockBuilder_check_inherents
            boolean goodToExecute = isBlockGoodToExecute(checkInherentsOutput);

            log.info("Block is good to execute: " + goodToExecute);

            if (goodToExecute) {
                runtime.call("Core_execute_block", executeBlockParameter);
                log.info("Block executed successfully");

                // Persist the updates to the trie structure
                AccessorHolder.getInstance().getBlockTrieAccessor().persistUpdates();
                blockState.setFinalizedHash(blockHeader.getHash(), BigInteger.ZERO, BigInteger.ZERO);
            } else {
                log.info("Block not executed");
                throw new BlockExecutionException();
            }
        }
    }

    /**
     * Checks whether a block is good to execute based on the output of BlockBuilder_check_inherents.
     *
     * @param checkInherentsOutput The output of BlockBuilder_check_inherents.
     * @return True if the block is good to execute, false otherwise.
     */
    private static boolean isBlockGoodToExecute(byte[] checkInherentsOutput) {
        var data =
                ScaleUtils.Decode.decode(ArrayUtils.subarray(checkInherentsOutput, 2, checkInherentsOutput.length),
                        new ListReader<>(new PairReader<>(scr -> new String(scr.readByteArray(8)),
                                scr -> new String(scr.readByteArray()))));

        boolean goodToExecute;

        if (data.size() > 1) {
            goodToExecute = false;
        } else if (data.size() == 1) {
            //If the inherent is babeslot or auraslot, then it's an expected issue and we can proceed
            goodToExecute =
                    data.get(0).getValue0().equals("babeslot") || data.get(0).getValue0().equals("auraslot");
        } else {
            goodToExecute = true;
        }
        return goodToExecute;
    }

    /**
     * Constructs the parameter for calling BlockBuilder_check_inherents.
     *
     * @param executeBlockParameter The parameter for executing the block.
     * @return The parameter for checking inherents.
     */
    public static byte[] getCheckInherentsParameter(byte[] executeBlockParameter) {
        // The first param is executeBlockParameter
        // The second param of `BlockBuilder_check_inherents` is a SCALE-encoded list of
        // tuples containing an "inherent identifier" (`[u8; 8]`) and a value (`Vec<u8>`).
        return ArrayUtils.addAll(executeBlockParameter, getInherentDataParameter());
    }

    /**
     * Constructs the parameter for inherent data.
     *
     * @return The parameter for inherent data.
     */
    public static byte[] getInherentDataParameter() {
        long millis = System.currentTimeMillis();
        return ScaleUtils.Encode.encode(new InherentDataWriter(), new InherentData(millis));
    }
}
