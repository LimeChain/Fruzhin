package com.limechain.sync.warpsync.state;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.lightclient.LightMessages;
import com.limechain.network.protocol.lightclient.LightMessagesProtocol;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log
public class RuntimeDownloadTest {
    @Disabled("This is an integration test")
    @Test
    public void runtimeDownloadAndBuildTest() {
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

            kademliaService.setHost(senderNode);
            var peerId = PeerId.fromBase58("12D3KooWFFqjBKoSdQniRpw1Y8W6kkV7takWv1DU2ZMkaA81PYVq");
            var receivers = new String[]{
//                    "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWPyKTVdykB9iEXEggRkbMTV4qsWywhtT42qz16eBTvReA"
                    "/dns/polkadot-boot-ng.dwellir.com/tcp/30336/p2p/" +
                            "12D3KooWFFqjBKoSdQniRpw1Y8W6kkV7takWv1DU2ZMkaA81PYVq",
            };

            kademliaService.connectBootNodes(receivers);

            //Block must not be older than 256 than the latest block
            LightClientMessage.Response response = lightMessages.remoteReadRequest(
                    senderNode,
                    kademliaService.getHost().getAddressBook(),
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

            System.out.println("Instantiating module");
            RuntimeBuilder.buildRuntime(code);

            log.log(Level.INFO, "Runtime and heap pages downloaded");
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(e);
        } finally {
            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
