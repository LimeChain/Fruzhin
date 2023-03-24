package com.limechain.network.kad;

import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

@Log
@Getter
@Setter
public class KademliaService {
    public static final int MIN_PEER = 20;
    private List<KademliaController> peers = new ArrayList<>();
    private Kademlia dht;
    private Host host;
    private HostBuilder hostBuilder;

    LinkedHashMap<String, String> peerAddresses;

    public KademliaService(String protocolId, List<String> bootstrapNodes, Multihash hostId, boolean localDht) {
        this.initialize(protocolId, hostId, localDht, bootstrapNodes);
    }

    private void initialize(String protocolId, Multihash hostId, boolean localEnabled, List<String> bootstrapNodes) {
        dht = new Kademlia(new KademliaEngine(hostId, new RamProviderStore(), new RamRecordStore()), protocolId,
                20, 3, localEnabled);
        peerAddresses = new LinkedHashMap<>();
        for (String bootNode : bootstrapNodes) {
            try {
                KademliaController kademliaController = dht.dial(host, Multiaddr.fromString(bootNode)).getController().join();
                peers.add(kademliaController);
            } catch (Exception e) {
                log.log(Level.WARNING, "Could not connect to bootstrap node: " + bootNode, e);
            }

            log.log(Level.INFO, String.format("Kad boostrap finished. Connected to %s peers", peers.size()));
        }
    }

    public void findNewPeers() {
        byte[] hash = new byte[32];
        (new Random()).nextBytes(hash);
        Multihash randomPeerId = new Multihash(Multihash.Type.sha2_256, hash);
        try {
            List<PeerAddresses> closestPeers = dht.findClosestPeers(randomPeerId, 20, host);

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
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error finding closest peers", e);
        }
        log.log(Level.INFO, "Total peers: " + peerAddresses.size());
    }
}
