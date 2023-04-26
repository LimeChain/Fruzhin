package com.limechain.network.kad;

import com.limechain.network.protocol.NetworkService;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.multistream.ProtocolBinding;
import lombok.extern.java.Log;
import org.peergos.PeerAddresses;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.protocol.dht.KademliaEngine;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service used for operating the Kademlia distributed hash table.
 */
@Log
public class KademliaService implements NetworkService {
    public static final int REPLICATION = 20;
    public static final int ALPHA = 3;
    public Host host;
    private Kademlia kademlia;

    public KademliaService(String protocolId, Multihash hostId, boolean localDht, boolean clientMode) {
        this.initialize(protocolId, hostId, localDht, clientMode);
    }

    public ProtocolBinding getProtocol() {
        return this.kademlia;
    }

    /**
     * Initializes Kademlia dht with replication=20 and alpha=3
     *
     * @param protocolId
     * @param hostId
     * @param localEnabled
     * @return Kademlia dht
     */
    private void initialize(String protocolId, Multihash hostId, boolean localEnabled, boolean clientMode) {
        kademlia = new Kademlia(new KademliaEngine(hostId, new RamProviderStore(), new RamRecordStore()),
                protocolId, REPLICATION, ALPHA, localEnabled, clientMode);
    }

    /**
     * Connects to boot nodes to the Kademlia dht
     *
     * @param bootNodes boot nodes set in ChainService
     * @return the number of successfully connected nodes
     */
    public int connectBootNodes(String[] bootNodes) {
        var bootstrapMultiAddress = Stream.of(bootNodes)
                .map(DnsUtils::dnsNodeToIp4)
                .map(MultiAddress::new)
                .collect(Collectors.toList());
        int successfulBootNodes = kademlia.bootstrapRoutingTable(host, bootstrapMultiAddress,
                addr -> !addr.contains("wss") && !addr.contains("ws"));
        if (successfulBootNodes > 0)
            log.log(Level.INFO, "Successfully connected to " + successfulBootNodes + " boot nodes");
        else log.log(Level.SEVERE, "Failed to connect to bootnodes");
        return successfulBootNodes;
    }

    /**
     * Populates Kademlia dht with peers closest in distance to a random id then makes connections with our node
     *
     * @return Successfully connected peers
     */
    public Map<Multihash, List<MultiAddress>> findNewPeers(HashMap<Multihash, List<MultiAddress>> connected) {
        byte[] hash = new byte[32];
        (new Random()).nextBytes(hash);
        Multihash randomPeerId = new Multihash(Multihash.Type.sha2_256, hash);
        var peers = kademlia.findClosestPeers(randomPeerId, REPLICATION, host);

        Map<Multihash, List<MultiAddress>> connectedPeers = new HashMap<>();
        Iterator peerIterator = peers.iterator();

        while (peerIterator.hasNext()) {
            PeerAddresses peer = (PeerAddresses) peerIterator.next();
            if (!connected.containsKey(peer.peerId) && kademlia.connectTo(host, peer)) {
                connectedPeers.put(peer.peerId, peer.addresses);
            }
        }

        // TODO: We can't connect to ws peers for now
        // Add filtering of /ws and /wss peers
        return connectedPeers;
    }
}