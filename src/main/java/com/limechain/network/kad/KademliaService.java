package com.limechain.network.kad;

import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.protocol.dht.KademliaEngine;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
@Getter
@Setter
public class KademliaService {
    public static final int REPLICATION = 20;
    public static final int ALPHA = 3;
    private Kademlia dht;
    private Host host;

    public KademliaService(String protocolId, Multihash hostId, boolean localDht) {
        this.initialize(protocolId, hostId, localDht);
    }

    private void initialize(String protocolId, Multihash hostId, boolean localEnabled) {
        dht = new Kademlia(new KademliaEngine(hostId, new RamProviderStore(), new RamRecordStore()),
                protocolId, REPLICATION, ALPHA, localEnabled);
    }

    public void connectBootNodes(String[] bootNodes) {
        var bootstrapMultiAddress = List.of(bootNodes).stream()
                .map(MultiAddress::new)
                .collect(Collectors.toList());
        dht.bootstrapRoutingTable(host, bootstrapMultiAddress, addr -> !addr.contains("wss"));
    }

    public void findNewPeers() {
        byte[] hash = new byte[32];
        (new Random()).nextBytes(hash);
        Multihash randomPeerId = new Multihash(Multihash.Type.sha2_256, hash);
        try {
            dht.findClosestPeers(randomPeerId, REPLICATION, host);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error finding closest peers", e);
        }
    }
}
