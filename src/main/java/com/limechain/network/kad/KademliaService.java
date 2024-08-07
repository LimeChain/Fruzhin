package com.limechain.network.kad;

import lombok.Getter;
import lombok.extern.java.Log;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Service used for operating the Kademlia distributed hash table.
 */
@Getter
@Log
public class KademliaService /*extends NetworkService<Kademlia>*/ {
    public static final int REPLICATION = 20;
    private static final int ALPHA = 3;
    private static final Random RANDOM = new Random();

//    @Setter
//    private Host host;
//    private List<PeerId> bootNodePeerIds;
    private int successfulBootNodes;

    public KademliaService(String protocolId, boolean localDht, boolean clientMode) {
        this.initialize(protocolId, localDht, clientMode);
    }

    /**
     * Initializes Kademlia dht with replication=20 and alpha=3
     *
     * @param protocolId
//     * @param hostId
     * @param localEnabled
     */
    private void initialize(String protocolId, boolean localEnabled, boolean clientMode) {
//        protocol = new Kademlia(
//                new KademliaEngine(hostId, new RamProviderStore(1000), new RamRecordStore(), new RamBlockstore()),
//                protocolId, REPLICATION, ALPHA, localEnabled, clientMode);
    }

    public void addReservedPeer(String multiaddr) throws ExecutionException, InterruptedException {
//        final Multiaddr addrWithPeer = Multiaddr.fromString(multiaddr);
//
//        CompletableFuture<Stream> peerStream =
//                protocol.dial(host, addrWithPeer.getPeerId(), addrWithPeer).getStream();
//
//        Stream stream = peerStream.get();
//        if (stream == null) {
//            log.log(Level.WARNING, "Failed to connect to reserved peer");
//        } else {
//            ConnectionManager.getInstance().addNewPeer(addrWithPeer.getPeerId());
//            log.log(Level.INFO, "Successfully connected to reserved peer");
//        }
    }

    /**
     * Connects to boot nodes to the Kademlia dht
     *
     * @param bootNodes boot nodes set in ChainService
     * @return the number of successfully connected nodes
     */
    public int connectBootNodes(String[] bootNodes) {
//        var bootstrapMultiAddress = Arrays.stream(bootNodes)
//                .map(DnsUtils::dnsNodeToIp4)
//                .map(MultiAddress::new)
//                .toList();
//        this.setBootNodePeerIds(bootNodes);
//        successfulBootNodes = protocol.bootstrapRoutingTable(host, bootstrapMultiAddress,
//                addr -> !addr.contains("wss") && !addr.contains("ws"));
        if (successfulBootNodes > 0)
            log.log(Level.INFO, "Successfully connected to " + successfulBootNodes + " boot nodes");
        else log.log(Level.SEVERE, "Failed to connect to boot nodes");
        return successfulBootNodes;
    }

    /**
     * Populates Kademlia dht with peers closest in distance to a random id then makes connections with our node
     */
    public void findNewPeers() {
//        protocol.findClosestPeers(randomPeerId(), REPLICATION, host);
//        final var peers =
//                protocol.findClosestPeers(Multihash.deserialize(host.getPeerId().getBytes()), REPLICATION, host);
//
//        peers.stream().parallel().forEach(p -> {
//            boolean isConnected = protocol.connectTo(host, p);
//
//            if (!isConnected) {
//                protocol.connectTo(host, p);
//            }
//        });
    }
//
//    private Multihash randomPeerId() {
//        byte[] hash = new byte[32];
//        RANDOM.nextBytes(hash);
//        return new Multihash(Multihash.Type.sha2_256, hash);
//    }
//
//    private void setBootNodePeerIds(String[] bootNodes) {
//        ArrayList<PeerId> ids = new ArrayList<>();
//        for (String bootNode : bootNodes) {
//            String peerId = bootNode.substring(bootNode.lastIndexOf('/') + 1, bootNode.length());
//            ids.add(PeerId.fromBase58(peerId));
//        }
//        this.bootNodePeerIds = ids;
//    }
}