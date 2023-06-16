package com.limechain.network.kad;

import com.limechain.network.protocol.NetworkService;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.ProtocolBinding;
import lombok.extern.java.Log;
import org.peergos.PeerAddresses;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.protocol.dht.KademliaEngine;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    public Kademlia kademlia;
    private final RamAddressBook addressBook = new RamAddressBook();

    public KademliaService(String protocolId, Multihash hostId, boolean localDht, boolean clientMode) {
        this.initialize(protocolId, hostId, localDht, clientMode);
    }

    public ProtocolBinding getProtocol() {
        return this.kademlia;
    }

    public Map<PeerId, Set<Multiaddr>> getAddresses() {
        return addressBook.getAddresses();
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
        kademlia.setAddressBook(addressBook);
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
        else log.log(Level.SEVERE, "Failed to connect to boot nodes");
        return successfulBootNodes;
    }

    /**
     * Populates Kademlia dht with peers closest in distance to a random id then makes connections with our node
     */
    public void findNewPeers() {
        List<PeerAddresses> peers = kademlia.findClosestPeers(randomPeerId(), REPLICATION, host);

        for (PeerAddresses peer : filterWsPeers(peers)) {
            if (getAddresses().size() >= REPLICATION) {
                log.log(Level.INFO, "Successfully reached " + REPLICATION + " peers");
                break;
            }

            connectToPeer(peer);
        }
    }

    private Multihash randomPeerId(){
        byte[] hash = new byte[32];
        new Random().nextBytes(hash);
        return new Multihash(Multihash.Type.sha2_256, hash);
    }

    private List<PeerAddresses> filterWsPeers(List<PeerAddresses> peerAddresses) {
        final var filteredAddresses = peerAddresses.stream()
                .map(this::filterWsAddresses)
                .filter(p -> !p.addresses.isEmpty())
                .toList();

        int filteredOut = peerAddresses.size() - filteredAddresses.size();
        if (filteredOut > 0) {
            log.log(Level.INFO,
                    "Filtered out " + filteredOut + " peers because of WS incompatibility");
        }

        return filteredAddresses;
    }

    private PeerAddresses filterWsAddresses(PeerAddresses peerAddresses) {
        return new PeerAddresses(
                peerAddresses.peerId,
                peerAddresses.addresses.stream()
                        .filter(a -> !a.toString().contains("/ws") && !a.toString().contains("/wss"))
                        .toList());
    }

    private void connectToPeer(PeerAddresses peerAddresses) {
        if (peerAlreadyInAddressBook(peerAddresses.peerId)) {
            log.log(Level.INFO, "Already connected to peer " + peerAddresses.peerId);
        }

        if (kademlia.connectTo(host, peerAddresses)) {
            log.log(Level.INFO, "Successfully connected to peer " + peerAddresses.peerId);
        }
    }

    private boolean peerAlreadyInAddressBook(Multihash peerId) {
        return getAddresses().containsKey(PeerId.fromHex(peerId.toHex()));
    }
}