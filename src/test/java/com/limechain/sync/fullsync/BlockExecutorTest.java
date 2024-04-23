package com.limechain.sync.fullsync;

import com.google.protobuf.ByteString;
import com.limechain.chain.spec.ChainSpec;
import com.limechain.client.FullNode;
import com.limechain.network.protocol.blockannounce.scale.BlockHeaderScaleWriter;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockBody;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.Extrinsics;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.network.protocol.warp.scale.reader.HeaderDigestReader;
import com.limechain.network.protocol.warp.scale.writer.BlockBodyWriter;
import com.limechain.rpc.server.RpcApp;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.trie.AccessorHolder;
import com.limechain.utils.StringUtils;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.PairReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Log
@ExtendWith(MockitoExtension.class)
class BlockExecutorTest {

    @BeforeEach
    public void setup() {
        // We need the rpc app in order to initialize the spring context we're so dependent on.
        new RpcApp().start("-n kusama --node-mode full --db-recreate --sync-mode full".split(" "));

        // Also, populating the DB with the genesis state is necessary.
        FullNode.initializeGenesis();

        // Set the state root to the genesis state root
        AccessorHolder.getInstance().setToStateRoot(StringUtils.hexToBytes("b0006203c3a6e6bd2c6a17b1d4ae8ca49a31da0f4579da950b127774b44aef6b"));
    }

    @Test
    void kusamaFirstBlock() throws IOException {

        byte[] scaleEncodedBody = new byte[]{8, 40, 4, 2, 0, 11, -112, 17, 14, -77, 110, 1, 16, 4, 20, 0, 0};
        byte[] scaleEncodedDigest = StringUtils.hexToBytes(
                "0x0c0642414245340201000000ef55a50f00000000044241424549040118ca239392960473fe1bc65f94ee27d890a49c1b200c006ff5dcc525330ecc16770100000000000000b46f01874ce7abbb5220e8fd89bede0adad14c73039d91e28e881823433e723f0100000000000000d684d9176d6eb69887540c9a89fa6097adea82fc4b0ff26d1062b488f352e179010000000000000068195a71bdde49117a616424bdc60a1733e96acb1da5aeab5d268cf2a572e94101000000000000001a0575ef4ae24bdfd31f4cb5bd61239ae67c12d4e64ae51ac756044aa6ad8200010000000000000018168f2aad0081a25728961ee00627cfe35e39833c805016632bf7c14da5800901000000000000000000000000000000000000000000000000000000000000000000000000000000054241424501014625284883e564bc1e4063f5ea2b49846cdddaa3761d04f543b698c1c3ee935c40d25b869247c36c6b8a8cbbd7bb2768f560ab7c276df3c62df357a7e3b1ec8d");

        BlockHeader header = new BlockHeader();
        header.setParentHash(Hash256.from("0xb0a8d493285c2df73290dfb7e61f870f17b41801197a149ca93654499ea3dafe"));
        header.setBlockNumber(BigInteger.valueOf(1));
        header.setStateRoot(Hash256.from("0xfabb0c6e92d29e8bb2167f3c6fb0ddeb956a4278a3cf853661af74a076fc9cb7"));
        header.setExtrinsicsRoot(Hash256.from("0xa35fb7f7616f5c979d48222b3d2fa7cb2331ef73954726714d91ca945cc34fd8"));
        header.setDigest(
                ScaleUtils.Decode.decodeList(scaleEncodedDigest, new HeaderDigestReader()).toArray(HeaderDigest[]::new));

        var exts = ScaleUtils.Decode.decodeList(scaleEncodedBody, ScaleCodecReader::readByteArray);
        assertEquals(2, exts.size());
        BlockBody body = new BlockBody(exts.stream().map(Extrinsics::new).toList());
        Block block = new Block(header, body);

        ChainSpec chainSpec = ChainSpec.newFromJSON("genesis/ksmcc3.json");
        var top = chainSpec.getGenesis().getTop();

        var runtimeByteCode = top.get(ByteString.copyFrom(":code".getBytes())).toByteArray();

//        // Dumps the .wasm file for manual conversion to .wat
//         FileUtils.writeByteArrayToFile(new File("/tmp/kusama1.wasm"), runtimeByteCode);

        Runtime genesisRuntime = new RuntimeBuilder().buildRuntime(runtimeByteCode);
        byte[] encodedUnsealedHeader =
                ScaleUtils.Encode.encode(BlockHeaderScaleWriter.getInstance()::writeUnsealed, block.getHeader());

        var encodedBody = ScaleUtils.Encode.encode(BlockBodyWriter.getInstance(), body);

        var executeBlockParam = ArrayUtils.addAll(encodedUnsealedHeader, encodedBody);
        var checkInherentsParam = FullSyncMachine.getCheckInherentsParameter(executeBlockParam);

        // Check_inherents is ok
        byte[] checkInherentsResponse = genesisRuntime.call("BlockBuilder_check_inherents", checkInherentsParam);

        // Parse an expected babeslot error (expected since older runtimes since think this inherent will be passed to them, but it's deprecated)
        boolean checkOk = checkInherentsResponse[0] == 1;
        boolean fatalErrorEncountered = checkInherentsResponse[1] == 1;
        var data = ScaleUtils.Decode.decode(ArrayUtils.subarray(checkInherentsResponse, 2, checkInherentsResponse.length),
            new ListReader<>(new PairReader<>(scr -> new String(scr.readByteArray(8)), scr -> new String(scr.readByteArray()))));

        // Assert that the first kusama block is properly encoded as a parameter to the runtime
        assertArrayEquals(
            StringUtils.hexToBytes("b0a8d493285c2df73290dfb7e61f870f17b41801197a149ca93654499ea3dafe04fabb0c6e92d29e8bb2167f3c6fb0ddeb956a4278a3cf853661af74a076fc9cb7a35fb7f7616f5c979d48222b3d2fa7cb2331ef73954726714d91ca945cc34fd8080642414245340201000000ef55a50f00000000044241424549040118ca239392960473fe1bc65f94ee27d890a49c1b200c006ff5dcc525330ecc16770100000000000000b46f01874ce7abbb5220e8fd89bede0adad14c73039d91e28e881823433e723f0100000000000000d684d9176d6eb69887540c9a89fa6097adea82fc4b0ff26d1062b488f352e179010000000000000068195a71bdde49117a616424bdc60a1733e96acb1da5aeab5d268cf2a572e94101000000000000001a0575ef4ae24bdfd31f4cb5bd61239ae67c12d4e64ae51ac756044aa6ad8200010000000000000018168f2aad0081a25728961ee00627cfe35e39833c805016632bf7c14da580090100000000000000000000000000000000000000000000000000000000000000000000000000000008280402000b90110eb36e011004140000"),
            executeBlockParam);

        // Attempt to execute a block
        log.info("Executing block");
        byte[] executeBlockResponse = genesisRuntime.call("Core_execute_block", executeBlockParam);

        System.out.println("done");
    }
}
