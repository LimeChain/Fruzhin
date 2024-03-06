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
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
public class FullSyncMachine {
    private final Network networkService;

//    private BlockExecutor blockExecutor;

    public FullSyncMachine(final Network networkService) {
        this.networkService = networkService;
    }

    public void start() {

        // TODO: DIRTY INITIALIZATION FIX:
        //  this.networkService.currentSelectedPeer is null,
        //  unless explicitly set via some of the "update..." methods
        this.networkService.updateCurrentSelectedPeerWithNextBootnode();

        // get response from block request
        final int HEADER = 0b0000_0001;
        final int BODY = 0b0000_0010;
        final int JUSTIFICATION = 0b0001_0000;
        SyncMessage.BlockResponse response = networkService.makeBlockRequest(new BlockRequestDto(
            HEADER | BODY | JUSTIFICATION,
            null, // no hash, number instead
            1, //we want to start from the first block
            SyncMessage.Direction.Descending,
            1 //let's say we only want the next block after the genesis
        ));

        // Get the block from the list of responses:
        List<SyncMessage.BlockData> receivedBlockDatas = response.getBlocksList();
        SyncMessage.BlockData blockData = receivedBlockDatas.get(0);

        // Protobuf decode the block header
        var encodedHeader = blockData.getHeader().toByteArray();
        BlockHeader blockHeader = ScaleUtils.Decode.decode(encodedHeader, new BlockHeaderReader());
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

        Runtime runtime = new RuntimeBuilder().buildRuntime(runtimeCode);

        // Call BlockBuilder_check_inherents:
        var args = getCheckInherentsParameter(executeBlockParameter);
        byte[] checkInherentsOutput = runtime.call("BlockBuilder_check_inherents", args);

        // TODO: CONTINUE FROM HERE
        //  Kusama block 1 seems to be decoded just about right, with slight mismatches somewhere...
        //  (index 97 in executeBlockParameter is 12 here, 6 in gossamer)
        //  Refer to this gossamer test for verification:
        //  https://github.com/ChainSafe/gossamer/blob/644b212ed3e4a133fbd9b069552b3f1d65e56012/lib/runtime/wazero/instance_test.go#L721
        System.out.println(Arrays.toString(checkInherentsOutput));
    }

    private static byte[] getCheckInherentsParameter(byte[] executeBlockParameter) {
        // The first param is executeBlockParameter
        // The second param of `BlockBuilder_check_inherents` is a SCALE-encoded list of
        // tuples containing an "inherent identifier" (`[u8; 8]`) and a value (`Vec<u8>`).
        return ArrayUtils.addAll(executeBlockParameter, getInherentDataParameter());
    }

    private static byte[] getInherentDataParameter() {
        long millis = System.currentTimeMillis();
        return ScaleUtils.Encode.encode(new InherentDataWriter(), new InherentData(millis));
    }
}
