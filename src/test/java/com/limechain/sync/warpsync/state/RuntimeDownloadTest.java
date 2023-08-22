package com.limechain.sync.warpsync.state;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.blockannounce.scale.BlockHeaderScaleWriter;
import com.limechain.network.protocol.lightclient.LightMessages;
import com.limechain.network.protocol.lightclient.LightMessagesProtocol;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.runtime.RuntimeVersion;
import com.limechain.sync.warpsync.scale.RuntimeVersionReader;
import com.limechain.trie.Trie;
import com.limechain.trie.TrieVerifier;
import com.limechain.trie.decoder.TrieDecoderException;
import com.limechain.utils.LittleEndianUtils;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.types.Hash256;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import lombok.extern.java.Log;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;
import org.wasmer.Memory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log
public class RuntimeDownloadTest {
    static Host senderNode = null;
    private static Runtime runtime = null;
    //Block must not be older than 256 than the latest block
    private static final String stateRootString = "0xcd95074fcf8e8ad450cef04d4d3ae851987f025faf0e8044a79720c3ce9e5730";
    private static final String blockHash = "0xfbbb584c1e55d38b2cad5f6d6efec84d659d527f6461ac37166c789c663f5f53";

    @BeforeAll
    public static void init() throws IOException {
        //Setup node and connect to boot nodes
        HostBuilder hostBuilder1 =
                (new HostBuilder()).generateIdentity().listenLocalhost(10000
                        + new Random().nextInt(50000));

        var lightMessages = new LightMessages("/dot/light/2", new LightMessagesProtocol());
        var kademliaService = new KademliaService("/dot/kad",
                Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false, true);

        hostBuilder1.addProtocols(List.of(new Ping(), lightMessages, kademliaService.getProtocol()));
        senderNode = hostBuilder1.build();

        senderNode.start().join();

        kademliaService.setHost(senderNode);
        var peerId = PeerId.fromBase58("12D3KooWFFqjBKoSdQniRpw1Y8W6kkV7takWv1DU2ZMkaA81PYVq");
        var receivers = new String[]{
                "/dns/polkadot-boot-ng.dwellir.com/tcp/30336/p2p/" +
                        "12D3KooWFFqjBKoSdQniRpw1Y8W6kkV7takWv1DU2ZMkaA81PYVq",
        };

        kademliaService.connectBootNodes(receivers);
        //Make a call to retrieve the runtime code information
        LightClientMessage.Response response = lightMessages.remoteReadRequest(
                senderNode,
                kademliaService.getHost().getAddressBook(),
                peerId,
                blockHash,
                new String[]{StringUtils.toHex(":code")}
        );
        assertNotNull(response);

        //Parse the runtime code so that we can build it in a module later
        byte[] codeKey = LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":code")));
        var res = response.getRemoteReadResponse();
        byte[] proof = res.getProof().toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(proof);
        int size = reader.readCompactInt();
        byte[][] decodedProofs = new byte[size][];
        for (int i = 0; i < size; ++i) {
            decodedProofs[i] = reader.readByteArray();
        }

        //Must change to the latest state root when updating block hash
        Hash256 stateRoot = Hash256.from(stateRootString);
        Trie trie;
        try {
            trie = TrieVerifier.buildTrie(decodedProofs, stateRoot.getBytes());
        } catch (TrieDecoderException e) {
            throw new RuntimeException("Couldn't build trie from proofs list");
        }
        var code = trie.get(codeKey);
        assertNotNull(code);

        //Build runtime
        System.out.println("Instantiating module");
        runtime = RuntimeBuilder.buildRuntime(code);
        coreInitialize();
    }

    public static void coreInitialize() throws IOException {
        Memory memory = runtime.getInstance().exports.getMemory("memory");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ScaleCodecWriter scaleCodecWriter = new ScaleCodecWriter(buf);
        BlockHeaderScaleWriter blockHeaderScaleWriter = new BlockHeaderScaleWriter();
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setParentHash(Hash256.from(blockHash));
        blockHeader.setBlockNumber(BigInteger.valueOf(16871500));
        blockHeader.setStateRoot(Hash256.from("0x0000000000000000000000000000000000000000000000000000000000000000"));
        blockHeader.setExtrinsicsRoot(Hash256.from("0x0000000000000000000000000000000000000000000000000000000000000000"));
        blockHeader.setDigest(new HeaderDigest[]{});
        blockHeaderScaleWriter.write(scaleCodecWriter, blockHeader);
        memory.buffer().put(buf.toByteArray());
        Object[] runtimeResponse = null;
        try {
            runtimeResponse = runtime.getInstance().exports.getFunction("Core_initialize_block")
                    .apply(0,0);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            long[] pointers = new long[]{30066183504l, 807453851884l};
            for (int i = 0; i < 2; i++) {
                long pointer = pointers[i];
                int ptr = (int) pointer;
                int ptrSize = (int) (pointer >> 32);
                byte[] data = new byte[ptrSize];
                memory.buffer().get(ptr, data, 0, ptrSize);
                System.out.println(new String(data));
            }
        }
        log.log(Level.INFO, "Runtime and heap pages downloaded");
    }

    @Test
    public void CodeVersionTest() throws IOException {
        Object[] runtimeResponse =
                runtime.getInstance().exports.getFunction("Core_version")
                        .apply(0, 0);
        long memPointer = (long) runtimeResponse[0];
        int ptr = (int) memPointer;
        int ptrLength = (int) (memPointer >> 32);
        Memory memory = runtime.getInstance().exports.getMemory("memory");
        byte[] data = new byte[ptrLength];
        memory.buffer().get(ptr, data, 0, ptrLength);
        ScaleCodecReader reader = new ScaleCodecReader(data);
        RuntimeVersionReader runtimeVersionReader = new RuntimeVersionReader();
        RuntimeVersion runtimeVersion = runtimeVersionReader.read(reader);
        log.log(Level.INFO, "Runtime and heap pages downloaded");
    }

    @Test
    public void BabeApiCurrentEpoch() {
        Object[] runtimeResponse =
                runtime.getInstance().exports.getFunction("BabeApi_current_epoch")
                        .apply(0, 0);
        long memPointer = (long) runtimeResponse[0];
        int ptr = (int) memPointer;
        int ptrLength = (int) (memPointer >> 32);
        Memory memory = runtime.getInstance().exports.getMemory("memory");
        byte[] data = new byte[ptrLength];
        memory.buffer().get(ptr, data, 0, ptrLength);
        log.log(Level.INFO, "Runtime and heap pages downloaded");
    }
}
