package com.limechain.network.kad;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.NetworkService;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.Stream;
import io.libp2p.core.multiformats.Multiaddr;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.protocol.dht.KademliaEngine;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Service used for operating the Kademlia distributed hash table.
 */
@Log
public class KademliaService extends NetworkService<Kademlia> {
    public static final int REPLICATION = 20;
    public static final int ALPHA = 3;

    @Setter
    @Getter
    private Host host;

    public KademliaService(String protocolId, Multihash hostId, boolean localDht, boolean clientMode) {
        this.initialize(protocolId, hostId, localDht, clientMode);
    }

    /**
     * Initializes Kademlia dht with replication=20 and alpha=3
     *
     * @param protocolId
     * @param hostId
     * @param localEnabled
     */
    private void initialize(String protocolId, Multihash hostId, boolean localEnabled, boolean clientMode) {
        protocol = new Kademlia(
                new KademliaEngine(hostId, new RamProviderStore(), new RamRecordStore(), new RamBlockstore()),
                protocolId, REPLICATION, ALPHA, localEnabled, clientMode);
    }

    public void addReservedPeer(String multiaddr) throws ExecutionException, InterruptedException {
        final Multiaddr addrWithPeer = Multiaddr.fromString(multiaddr);

        CompletableFuture<Stream> peerStream =
                protocol.dial(host, addrWithPeer.getPeerId(), addrWithPeer).getStream();

        Stream stream = peerStream.get();
        if (stream == null) {
            log.log(Level.WARNING, "Failed to connect to reserved peer");
        } else {
            ConnectionManager.getInstance().addNewPeer(addrWithPeer.getPeerId());
            log.log(Level.INFO, "Successfully connected to reserved peer");
        }
    }

    /**
     * Connects to boot nodes to the Kademlia dht
     *
     * @param bootNodes boot nodes set in ChainService
     * @return the number of successfully connected nodes
     */
    public int connectBootNodes(String[] bootNodes) {
        var bootstrapMultiAddress = Arrays.stream(bootNodes)
                .map(DnsUtils::dnsNodeToIp4)
                .map(MultiAddress::new)
                .toList();
        int successfulBootNodes = protocol.bootstrapRoutingTable(host, bootstrapMultiAddress,
                addr -> !addr.contains("wss") && !addr.contains("ws"));
        if (successfulBootNodes > 0)
            log.log(Level.INFO, "Successfully connected to " + successfulBootNodes + " boot nodes");
        else log.log(Level.SEVERE, "Failed to connect to boot nodes");
        return successfulBootNodes;
    }

    /**
     * Populates Kademlia dht with peers closest in distance to a random id then makes connections with our node
     */
    public void findNewPeers() {
        protocol.findClosestPeers(randomPeerId(), REPLICATION, host);
        final var peers =
                protocol.findClosestPeers(Multihash.deserialize(host.getPeerId().getBytes()), REPLICATION, host);

        peers.stream().parallel().forEach(p -> {
            boolean isConnected = protocol.connectTo(host, p);

            if (!isConnected) {
                protocol.connectTo(host, p);
            }
        });
    }

    private Multihash randomPeerId() {
        byte[] hash = new byte[32];
        new Random().nextBytes(hash);
        return new Multihash(Multihash.Type.sha2_256, hash);
    }
}