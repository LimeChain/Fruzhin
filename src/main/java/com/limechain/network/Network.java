package com.limechain.network;

import com.limechain.chain.Chain;
import com.limechain.chain.ChainService;
import com.limechain.config.HostConfig;
import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.lightclient.LightMessagesService;
import com.limechain.network.protocol.sync.SyncService;
import com.limechain.network.protocol.warp.WarpSyncService;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.protocol.Ping;
import lombok.extern.java.Log;
import org.peergos.HostBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A Singleton Network class that handles all peer connections and Kademlia
 */
@Component
@Log
public class Network {
    private static final int HOST_PORT = 30333;
    private static Network network;
    public SyncService syncService;
    public LightMessagesService lightMessagesService;
    public WarpSyncService warpSyncService;
    public KademliaService kademliaService;
    private Host host;

    /**
     * Initializes a host for the peer connection,
     * Initializes the Kademlia service
     * Manages if nodes running locally are going to be allowed
     * Connects Kademlia to boot nodes
     *
     * @param chainService chain specification information containing boot nodes
     * @param hostConfig   host configuration containing current network
     */
    private Network(ChainService chainService, HostConfig hostConfig) {
        boolean isLocalEnabled = hostConfig.getChain() == Chain.LOCAL;
        HostBuilder hostBuilder = (new HostBuilder()).generateIdentity().listenLocalhost(HOST_PORT);
        Multihash hostId = Multihash.deserialize(hostBuilder.getPeerId().getBytes());

        //TODO: Add new protocolId format with genesis hash
        String chainId = chainService.getGenesis().getProtocolId();
        String legacyKadProtocolId = String.format("/%s/kad", chainId);
        String legacyWarpProtocolId = String.format("/%s/sync/warp", chainId);
        String legacyLightProtocolId = String.format("/%s/light/2", chainId);
        String legacySyncProtocolId = String.format("/%s/sync/2", chainId);

        kademliaService = new KademliaService(legacyKadProtocolId, hostId, isLocalEnabled);
        lightMessagesService = new LightMessagesService(legacyLightProtocolId);
        warpSyncService = new WarpSyncService(legacyWarpProtocolId);
        syncService = new SyncService(legacySyncProtocolId);

        hostBuilder.addProtocols(
                List.of(
                        new Ping(),
                        kademliaService.getProtocol(),
                        lightMessagesService.getProtocol(),
                        warpSyncService.getProtocol(),
                        syncService.getProtocol()
                ));

        host = hostBuilder.build();

        kademliaService.host = host;
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
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void findPeers() {
        log.log(Level.INFO, "Searching for nodes...");
        kademliaService.findNewPeers();
    }
}
