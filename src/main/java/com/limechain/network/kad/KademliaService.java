package com.limechain.network.kad;

import com.limechain.network.protocol.NetworkService;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import lombok.extern.java.Log;
import org.peergos.PeerAddresses;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.protocol.dht.KademliaEngine;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;

import java.util.Random;
import java.util.concurrent.ExecutionException;
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

    public Kademlia getProtocol() {
        return this.kademlia;
    }

    /**
     * Initializes Kademlia dht with replication=20 and alpha=3
     *
     * @param protocolId
     * @param hostId
     * @param localEnabled
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

    public void findNewPeers() {
        kademlia.findClosestPeers(randomPeerId(), REPLICATION, host).stream()
                .filter(this::peerNotInAddressBook)
                .forEach(this::connectToAddresses);
    }

    private Multihash randomPeerId(){
        byte[] hash = new byte[32];
        (new Random()).nextBytes(hash);
        return new Multihash(Multihash.Type.sha2_256, hash);
    }

    private boolean peerNotInAddressBook(PeerAddresses peerAddresses) {
        final var addressBook = host.getAddressBook();

        try {
            return addressBook.getAddrs(PeerId.fromHex(peerAddresses.peerId.toHex())).get().isEmpty();
        } catch (InterruptedException | ExecutionException e) {
            log.log(Level.WARNING, "Interrupted while searching Address Book for peerId " + peerAddresses.peerId);
        }

        return true;
    }

    private void connectToAddresses(PeerAddresses peerAddresses) {
        kademlia.connectTo(host, peerAddresses);
    }
}