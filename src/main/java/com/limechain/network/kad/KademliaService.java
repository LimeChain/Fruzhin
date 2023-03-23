package com.limechain.network.kad;

import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.protocol.Ping;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.peergos.HostBuilder;
import org.peergos.PeerAddresses;
import org.peergos.protocol.autonat.AutonatProtocol;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.protocol.dht.KademliaController;
import org.peergos.protocol.dht.KademliaEngine;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

@Log
@Getter
@Setter
public class KademliaService {

    private List<KademliaController> peers = new ArrayList<>();
    private Kademlia dht;
    private Host host;
    private HostBuilder hostBuilder;

    public KademliaService(String protocolId, List<String> bootstrapNodes) {
        this.initialize(protocolId, bootstrapNodes);
    }

    private void initialize(String protocolId, List<String> boostrapNodes) {
        hostBuilder = (new HostBuilder()).generateIdentity().listenLocalhost(1001);
        Multihash peerId = Multihash.deserialize(hostBuilder.getPeerId().getBytes());
        dht = new Kademlia(new KademliaEngine(peerId, new RamProviderStore(), new RamRecordStore()), protocolId);
        hostBuilder.addProtocols(List.of(new Ping(), new AutonatProtocol.Binding(), dht));
        host = hostBuilder.build();

        for (String bootNode : boostrapNodes) {
            try {
                KademliaController dialResult = dht.dial(host, Multiaddr.fromString(bootNode)).getController().join();
                peers.add(dialResult);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to connect to boot node", e);
            }
        }
        log.log(Level.INFO, String.format("Kad boostrap finished. Connected to %s peers", peers.size()));
    }

    public void findNewPeers(LinkedHashMap<String, String> peerAddresses) {
        byte[] hash = new byte[32];
        (new Random()).nextBytes(hash);
        Multihash randomPeerId = new Multihash(Multihash.Type.sha2_256, hash);
        try {
            List<PeerAddresses> closestPeers = dht.findClosestPeers(randomPeerId, 10, host);

            for (PeerAddresses peer : closestPeers) {
                String peerId = peer.peerId.toString();
                if (!peer.addresses.contains(peerAddresses.get(peerId))) {
                    log.log(Level.INFO, peer.peerId.toString());
                    for (MultiAddress ma : peer.addresses) {
                        String connectionAddress = ma.toString() + "/p2p/" + peerId;
                        try {
                            dht.dial(host, Multiaddr.fromString(connectionAddress)).getController().join();
                            peerAddresses.put(peer.peerId.toString(), connectionAddress);
                            break;
                        } catch (Exception e) {
                            log.log(Level.INFO, "Error connecting to: " + connectionAddress, e);
                        }
                    }
                }
            }
        } catch (Exception e){
            log.log(Level.SEVERE, "Error finding closest peers", e);
        }
    }
}
