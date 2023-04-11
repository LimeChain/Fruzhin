package com.limechain.network;

import com.limechain.chain.Chain;
import com.limechain.chain.ChainService;
import com.limechain.config.HostConfig;
import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.sync.SyncService;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.protocol.Ping;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.peergos.HostBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Level;

/**
 * A Singleton Network class that handles all peer connections and Kademlia
 */
@Component
@Log
@Getter
@Setter
public class Network {
    /**
     * Interval between periodic peer searches
     */
    private static final int TEN_SECONDS_IN_MS = 10000;
    private static final int HOST_PORT = 1001;
    private static Network network;
    public SyncService syncService;
    public static KademliaService kademliaService;
    private HostBuilder hostBuilder;
    private Host host;

    /**
     * Initializes a host for the peer connection,
     * Initializes the Kademlia service
     * Manages if nodes running locally are going to be allowed
     * Connects Kademlia to boot nodes
     *
     * @param chainService chain specification information containing boot nodes
     * @param hostConfig host configuration containing current network
     */
    private Network(ChainService chainService, HostConfig hostConfig) {
        boolean isLocalEnabled = hostConfig.getChain() == Chain.LOCAL;
        hostBuilder = (new HostBuilder()).generateIdentity().listenLocalhost(HOST_PORT);
        Multihash hostId = Multihash.deserialize(hostBuilder.getPeerId().getBytes());
        kademliaService = new KademliaService("/dot/kad", hostId, isLocalEnabled);
        hostBuilder.addProtocols(List.of(new Ping(), kademliaService.getDht()));
        syncService = new SyncService();

        hostBuilder.addProtocols(List.of(new Ping(), kademliaService.getDht(), syncService.getSyncMessages()));

        host = hostBuilder.build();
        kademliaService.setHost(host);

        kademliaService.connectBootNodes(chainService.getGenesis().getBootNodes());
    }

    /**
     * @return Network class instance
     */
    public static Network getInstance() {
        if (network != null) {
            return network;
        }
        throw new AssertionError("Network not initialized.");
    }

    /**
     * Initializes singleton Network instance
     * This is used two times on startup
     *
     * @return Network instance saved in class or if not found returns new Network instance
     */
    public static Network initialize(ChainService chainService, HostConfig hostConfig) {
        if (network != null) {
            log.log(Level.WARNING, "Network module already initialized.");
            return network;
        }
        network = new Network(chainService, hostConfig);
        log.log(Level.INFO, "Initialized network module!");
        return network;
    }

    /**
     * Periodically searched for new peers
     */
    @Scheduled(fixedDelay = TEN_SECONDS_IN_MS)
    public void findPeers() {
        log.log(Level.INFO, "Searching for nodes...");
        kademliaService.findNewPeers();
    }
}
