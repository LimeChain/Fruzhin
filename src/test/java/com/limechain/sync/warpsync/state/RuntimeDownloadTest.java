package com.limechain.sync.warpsync.state;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.lightclient.LightMessages;
import com.limechain.network.protocol.lightclient.LightMessagesProtocol;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.runtime.RuntimeVersion;
import com.limechain.sync.warpsync.scale.RuntimeVersionReader;
import com.limechain.trie.Trie;
import com.limechain.trie.TrieVerifier;
import com.limechain.trie.decoder.TrieDecoderException;
import com.limechain.utils.LittleEndianUtils;
import com.limechain.utils.RandomGenerationUtils;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import lombok.extern.java.Log;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log
@Disabled
public class RuntimeDownloadTest {
    static Host senderNode = null;
    private static Runtime runtime = null;
    //Block must not be older than 256 than the latest block
    private static final String STATE_ROOT_STRING
            = "0x9343bea828cc470e94b6674b53c24cf3daea7ea5f304aa70485816f44350b084";
    private static final String BLOCK_HASH
            = "0xe5061e90ffa24b51dc1e9e4ccb1baedb7ba776da8fd4e117ac8b9085837d6ef6";

    private static final PeerId PEER_ID = PeerId.fromBase58("12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5");
    private static final String[] RECEIVERS = new String[]{
            "/dns/p2p.0.polkadot.network/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5",
    };

    @BeforeAll
    public static void init() {
        //Setup node and connect to boot nodes
        MultiAddress address = RandomGenerationUtils.generateRandomAddress();
        HostBuilder hostBuilder1 =
                (new HostBuilder()).generateIdentity().listen(List.of(address));

        var lightMessages = new LightMessages("/dot/light/2", new LightMessagesProtocol());
        var kademliaService = new KademliaService("/dot/kad",
                Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false, true);

        hostBuilder1.addProtocols(List.of(new Ping(), lightMessages, kademliaService.getProtocol()));
        senderNode = hostBuilder1.build();

        senderNode.start().join();

        kademliaService.setHost(senderNode);

        kademliaService.connectBootNodes(RECEIVERS);
        //Make a call to retrieve the runtime code information
        LightClientMessage.Response response = lightMessages.remoteReadRequest(
                senderNode,
                kademliaService.getHost().getAddressBook(),
                PEER_ID,
                BLOCK_HASH,
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
        Hash256 stateRoot = Hash256.from(STATE_ROOT_STRING);
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
    }

    @Test
    public void CoreVersionTest() {
        byte[] response =
                runtime.call("Core_version");
        assertNotNull(response);
        ScaleCodecReader reader = new ScaleCodecReader(response);
        RuntimeVersionReader runtimeVersionReader = new RuntimeVersionReader();
        RuntimeVersion runtimeVersion = runtimeVersionReader.read(reader);
        log.log(Level.INFO, "Core Version Decoded");
    }
}
