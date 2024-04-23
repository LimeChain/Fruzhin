package com.limechain.sync.fullsync;

import com.google.protobuf.ByteString;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.scale.BlockHeaderScaleWriter;
import com.limechain.network.protocol.sync.BlockRequestDto;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.sync.fullsync.inherents.InherentData;
import com.limechain.sync.fullsync.inherents.scale.InherentDataWriter;
import com.limechain.trie.AccessorHolder;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Objects;

@Getter
@Log
public class FullSyncMachine {
    private final Network networkService;

    public FullSyncMachine(final Network networkService) {
        this.networkService = networkService;
    }

    public void start() {
        // TODO: DIRTY INITIALIZATION FIX:
        //  this.networkService.currentSelectedPeer is null,
        //  unless explicitly set via some of the "update..." methods
        this.networkService.updateCurrentSelectedPeerWithNextBootnode();
        AccessorHolder.getInstance().setToGenesis(); //todo: dirty fix for now

        int startNumber = 1;
        List<SyncMessage.BlockData> receivedBlockDatas = getBlocks(startNumber, 10);
        while (!receivedBlockDatas.isEmpty()) {
            startNumber += 10;
            executeBlocks(receivedBlockDatas);
            receivedBlockDatas = getBlocks(startNumber, 10);
        }
    }

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

    private void executeBlocks(List<SyncMessage.BlockData> receivedBlockDatas) {
        for (SyncMessage.BlockData blockData : receivedBlockDatas) {
            // Protobuf decode the block header
            var encodedHeader = blockData.getHeader().toByteArray();
            BlockHeader blockHeader = ScaleUtils.Decode.decode(encodedHeader, new BlockHeaderReader());
            log.info("Block number to be executed is " + blockHeader.getBlockNumber());
            byte[] encodedUnsealedHeader =
                    ScaleUtils.Encode.encode(BlockHeaderScaleWriter.getInstance()::writeUnsealed, blockHeader);

            // Protobuf decode the block body and scale encode it
            var extrinsincs = blockData.getBodyList();
            var encodedBody = ScaleUtils.Encode.encodeAsList(
                    ScaleCodecWriter::writeByteArray,
                    () -> extrinsincs.stream().map(ByteString::toByteArray).iterator()
            );

            byte[] executeBlockParameter = ArrayUtils.addAll(encodedUnsealedHeader, encodedBody);

            // Begin with runtime calls
            // TODO: Fetch the bytecode better
            byte[] runtimeCode = Objects.requireNonNull(AppBean.getBean(GenesisBlockHash.class)
                            .getGenesisTrie()
                            .node(Nibbles.fromBytes(":code".getBytes()))
                            .asNodeHandle()
                            .getUserData())
                    .getValue();

            Runtime runtime = new RuntimeBuilder()
                    .buildRuntime(runtimeCode);

            // Call BlockBuilder_check_inherents:
            var args = getCheckInherentsParameter(executeBlockParameter);
            byte[] checkInherentsOutput = runtime.call("BlockBuilder_check_inherents", args);
            boolean checkOk = checkInherentsOutput[0] == 1;
            log.info("Block is good to execute: " + checkOk);

            runtime.call("Core_execute_block", executeBlockParameter);
            log.info("Block executed successfully");
            AccessorHolder.getInstance().getBlockTrieAccessor().persistAll();
        }
    }

    public static byte[] getCheckInherentsParameter(byte[] executeBlockParameter) {
        // The first param is executeBlockParameter
        // The second param of `BlockBuilder_check_inherents` is a SCALE-encoded list of
        // tuples containing an "inherent identifier" (`[u8; 8]`) and a value (`Vec<u8>`).
        return ArrayUtils.addAll(executeBlockParameter, getInherentDataParameter());
    }

    public static byte[] getInherentDataParameter() {
        long millis = System.currentTimeMillis();
        return ScaleUtils.Encode.encode(new InherentDataWriter(), new InherentData(millis));
    }
}
