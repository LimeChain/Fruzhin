package com.limechain.sync.warpsync.state;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.lightclient.LightMessages;
import com.limechain.network.protocol.lightclient.LightMessagesProtocol;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.sync.warpsync.Runtime;
import com.limechain.sync.warpsync.RuntimeBuilder;
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
                    "0x5203002e03949672589ad08b9115f39c2878ef254d879459ef25471377cb9695",
                    new String[]{StringUtils.toHex(":code")}
            );

            assertNotNull(response);

            byte[] codeKey = LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":code")));
            var res = response.getRemoteReadResponse();
            byte[] proof = res.getProof().toByteArray();

            //Must change to the latest state root when updating block hash
            Hash256 stateRoot = Hash256.from("0x50554f247e227e5ba097e4172c8d55e4ac6fcc53270db5272266de87f6c2325f");
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

            System.out.println("Calling exported function 'Core_initialize_block'" +
                    " as it calls both of the imported functions");
            runtime.getInstance().exports.getFunction("Core_initialize_block").apply(1, 2);

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
