package com.limechain.sync.warpsync.state;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.lightclient.LightMessages;
import com.limechain.network.protocol.lightclient.LightMessagesProtocol;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.trie.Trie;
import com.limechain.trie.TrieVerifier;
import com.limechain.trie.decoder.TrieDecoderException;
import com.limechain.utils.LittleEndianUtils;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;
import org.wasmer.ImportObject;
import org.wasmer.Imports;
import org.wasmer.Instance;
import org.wasmer.Module;
import org.wasmer.Type;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log
public class RuntimeDownloadStateTest {
    //@Disabled("Currently cannot get heap pages from built trie")
    @Test
    public void remoteReadRequest_return_response() {
        Host senderNode = null;
        try {
            File f = new File("/Users/boris/Dev/java-host/build/libwasmer_jni.dylib");
            System.load(f.getAbsolutePath());
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000
                            + new Random().nextInt(50000));

            var lightMessages = new LightMessages("/dot/light/2", new LightMessagesProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false, true);

            hostBuilder1.addProtocols(List.of(new Ping(), lightMessages, kademliaService.getProtocol()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.host = senderNode;
            var peerId = PeerId.fromBase58("12D3KooWFFqjBKoSdQniRpw1Y8W6kkV7takWv1DU2ZMkaA81PYVq");
            var receivers = new String[]{
//                    "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWPyKTVdykB9iEXEggRkbMTV4qsWywhtT42qz16eBTvReA"
                    "/dns/polkadot-boot-ng.dwellir.com/tcp/30336/p2p/12D3KooWFFqjBKoSdQniRpw1Y8W6kkV7takWv1DU2ZMkaA81PYVq",
            };

            kademliaService.connectBootNodes(receivers);

            //Block must not be older than 256 than the latest block
            LightClientMessage.Response response = lightMessages.remoteReadRequest(
                    senderNode,
                    kademliaService.host.getAddressBook(),
                    peerId,
                    "0x883d076cebb8de43145b5f6d9b9ceb93675073d03eb78ab5e3ab3b6f65d18e8a",
                    new String[]{StringUtils.toHex(":code"), StringUtils.toHex(":heappages")}
            );

            assertNotNull(response);

            byte[] heapPagesKey = LittleEndianUtils.convertBytes(
                    StringUtils.hexToBytes(StringUtils.toHex(":heappages")));
            byte[] codeKey = LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":code")));
            var res = response.getRemoteReadResponse();
            byte[] proof = res.getProof().toByteArray();
            Hash256 stateRoot = Hash256.from("0x5a36ab90a2c9ee1b9a6863591e5c9bab38a34f9b54cc34adedabb6174fdd108b");
            ScaleCodecReader reader = new ScaleCodecReader(proof);
            int size = reader.readCompactInt();
            byte[][] decodedProofs = new byte[size][];
            for (int i = 0; i < size; ++i) {
                decodedProofs[i] = reader.readByteArray();
            }

            Trie trie;
            try {
                trie = TrieVerifier.buildTrie(decodedProofs, stateRoot.getBytes());
            } catch (TrieDecoderException e) {
                throw new RuntimeException("Couldn't build trie from proofs list");
            }
            var code = trie.get(codeKey);
            assertNotNull(code);
            FileUtils.writeByteArrayToFile(new File("./wasm_code"), code);

            //assertNotNull(heapPages);
            Module module = new Module(code);
            Imports imports = Imports.from(Arrays.asList(
                    new ImportObject.FuncImport("env", "ext_storage_set_version_1", argv -> {
                        System.out.println("Message printed in the body of 'ext_storage_set_version_1'");
                        return argv;
                    }, Arrays.asList(Type.I64, Type.I64), Collections.emptyList()),
                    new ImportObject.FuncImport("env", "ext_storage_get_version_1", argv -> {
                        System.out.println("Message printed in the body of 'ext_storage_get_version_1'");
                        return argv;
                    }, Collections.singletonList(Type.I64), Collections.singletonList(Type.I64)),
                    new ImportObject.MemoryImport("env", 20, false)), module);
            System.out.println("Instantiating module");
            Instance instance = module.instantiate(imports);

            System.out.println("Calling exported function 'Core_initialize_block' as it calls both of the imported functions");
            instance.exports.getFunction("Core_initialize_block").apply(1, 2);

            log.log(Level.INFO, "Runtime and heap pages downloaded");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
