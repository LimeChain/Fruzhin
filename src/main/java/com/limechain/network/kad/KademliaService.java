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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Service used for operating the Kademlia distributed hash table.
 */
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

    /**
     * Initializes Kademlia dht with replication=20 and alpha=3
     *
     * @param protocolId
     * @param hostId
     * @param localEnabled
     * @return Kademlia dht
     */
    private void initialize(String protocolId, Multihash hostId, boolean localEnabled) {
        dht = new Kademlia(new KademliaEngine(hostId, new RamProviderStore(), new RamRecordStore()),
                protocolId, REPLICATION, ALPHA, localEnabled);
    }

    /**
     * Connects to boot nodes to the Kademlia dht
     *
     * @param bootNodes boot nodes set in ChainService
     */
    public void connectBootNodes(String[] bootNodes) {
        var bootstrapMultiAddress = List.of(bootNodes).stream()
                .map(KademliaService::dnsNodeToIp4)
                .map(MultiAddress::new)
                .collect(Collectors.toList());
        int successfulBootNodes = dht.bootstrapRoutingTable(host, bootstrapMultiAddress, addr -> !addr.contains("wss"));
        log.log(Level.INFO, "Successfully connected to " + successfulBootNodes + " boot nodes");
    }

    /**
     * Populates Kademlia dht with peers closest in distance to a random id
     */
    public void findNewPeers() {
        byte[] hash = new byte[32];
        (new Random()).nextBytes(hash);
        Multihash randomPeerId = new Multihash(Multihash.Type.sha2_256, hash);
        dht.findClosestPeers(randomPeerId, REPLICATION, host);
    }

    /**
     * Makes a dns lookup and changes the address to an equal ip4 address
     * Implementation is necessary due to a bug in jvm-libp2p that involves resolving dns addresses
     * https://github.com/Peergos/nabu/issues/22#issuecomment-1495687079
     *
     * @param bootNode
     * @return bootNode in ip4 format
     */
    public static String dnsNodeToIp4(String bootNode) {
        int prefixEnd = bootNode.indexOf('/', 1) + 1;
        String prefix = bootNode.substring(0, prefixEnd);

        if (prefix.equals("/dns/")) {
            int domainEnd = bootNode.indexOf('/', prefixEnd);
            String domain = bootNode.substring(prefixEnd, domainEnd);
            String postfix = bootNode.substring(domainEnd);

            try {
                InetAddress address = InetAddress.getByName(domain);
                bootNode = "/ip4/" + address.getHostAddress() + postfix;
            } catch (UnknownHostException e) {
                log.log(Level.WARNING, "Unknown domain for bootstrap node address", e);
            }
        }
        return bootNode;
    }
}
