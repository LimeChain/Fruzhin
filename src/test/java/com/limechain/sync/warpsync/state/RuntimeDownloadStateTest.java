package com.limechain.sync.warpsync.state;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.lightclient.LightMessages;
import com.limechain.network.protocol.lightclient.LightMessagesProtocol;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.sync.warpsync.runtime.Runtime;
import com.limechain.sync.warpsync.runtime.RuntimeBuilder;
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log
public class RuntimeDownloadStateTest {
    @Disabled("This is an integration test")
    @Test
    public void remoteReadRequest_return_response() {
        Host senderNode = null;
        try {
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
                    "/dns/polkadot-boot-ng.dwellir.com/tcp/30336/p2p/" +
                            "12D3KooWFFqjBKoSdQniRpw1Y8W6kkV7takWv1DU2ZMkaA81PYVq",
            };

            kademliaService.connectBootNodes(receivers);
            Thread.sleep(1000);

            //Block must not be older than 256 than the latest block
            LightClientMessage.Response response = lightMessages.remoteReadRequest(
                    senderNode,
                    kademliaService.host.getAddressBook(),
                    peerId,
                    "0x8aa206d2dc0386ac0c6e1c4033f2445f209d14b9a11861d6eda4787651705231",
                    new String[]{StringUtils.toHex(":code")}
            );

            assertNotNull(response);

            byte[] codeKey = LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":code")));
            var res = response.getRemoteReadResponse();
            byte[] proof = res.getProof().toByteArray();

            //Must change to the latest state root when updating block hash
            Hash256 stateRoot = Hash256.from("0xdfe82c05dd0f9cf5ade3f5544c4311a123627f13dcdb6864bc842a1eae28c7c5");
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
            System.out.println("Instantiating module");
            Runtime runtime = RuntimeBuilder.buildRuntime(code);

            System.out.println("Calling exported function...");
//            Thread.sleep(2000);
//            Memory memory = runtime.getInstance().exports.getMemory("memory");
//            ByteBuffer memoryBuffer = memory.buffer();
//            memoryBuffer.position(0);
//            System.out.println(HexUtils.
//                    fromHexString("0xebdf9b472717dea63c2ae4ae312229d90dadd3e5a4858464da80103bd0b033a7").length);
//
//            memoryBuffer.put((byte)HexUtils
//                    .fromHexString("0xebdf9b472717dea63c2ae4ae312229d90dadd3e5a4858464da80103bd0b033a7").length);
//            memoryBuffer.put(HexUtils
//                    .fromHexString("0xebdf9b472717dea63c2ae4ae312229d90dadd3e5a4858464da80103bd0b033a7"));
//            memoryBuffer.put(blockNumberToByteArray(16454328));
//            memoryBuffer.put(new byte[]{0});
//            memoryBuffer.put(new byte[]{0});
//            memoryBuffer.put(new byte[]{0});
//            Object runtimeResponse = runtime.getInstance().exports.getFunction("Core_version")
//                    .apply(
//                            new Object[]{
//                                0,0
//                            });

            log.log(Level.INFO, "Runtime and heap pages downloaded");
        } catch (IOException | UnsatisfiedLinkError e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
