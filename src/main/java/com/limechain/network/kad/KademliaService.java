package com.limechain.network.kad;

import io.ipfs.multihash.Multihash;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.protocol.Ping;
import lombok.extern.java.Log;
import org.peergos.HostBuilder;
import org.peergos.protocol.autonat.AutonatProtocol;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.protocol.dht.KademliaController;
import org.peergos.protocol.dht.KademliaEngine;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Log
public class KademliaService {

    public List<KademliaController> peers = new ArrayList<>();

    public KademliaService(String protocolId, List<String> bootstrapNodes) {
        this.initialize(protocolId, bootstrapNodes);
    }

    private void initialize(String protocolId, List<String> boostrapNodes) {
        HostBuilder hostBuilder = (new HostBuilder()).generateIdentity().listenLocalhost(1001);
        Multihash peerId = Multihash.deserialize(hostBuilder.getPeerId().getBytes());
        Kademlia dht =
                new Kademlia(new KademliaEngine(peerId, new RamProviderStore(), new RamRecordStore()), protocolId, 20,
                        3);
        hostBuilder.addProtocols(List.of(new Ping(), new AutonatProtocol.Binding(), dht));
        var host = hostBuilder.build();

        for (String bootNode : boostrapNodes) {
            try {
                KademliaController dialResult =
                        dht.dial(host, Multiaddr.fromString(bootNode)).getController().join();

                peers.add(dialResult);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to connect to boot node", e);
            }
        }
        log.log(Level.INFO, String.format("Kad boostrap finished. Connected to %s peers", peers.size()));

    }
}
