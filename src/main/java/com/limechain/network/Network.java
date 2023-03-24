package com.limechain.network;

import com.limechain.chain.Chain;
import com.limechain.chain.ChainService;
import com.limechain.config.HostConfig;
import com.limechain.network.kad.KademliaService;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.protocol.Ping;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.peergos.HostBuilder;
import org.peergos.protocol.autonat.AutonatProtocol;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Level;

@Component
@Log
@Getter
@Setter
public class Network {
    private static Network network;
    public static KademliaService kademliaService;

    private HostBuilder hostBuilder;
    private Host host;

    private Network(ChainService chainService, HostConfig hostConfig) {
        boolean isLocalEnabled = hostConfig.getChain() == Chain.LOCAL;
        hostBuilder = (new HostBuilder()).generateIdentity().listenLocalhost(1001);
        Multihash hostId = Multihash.deserialize(hostBuilder.getPeerId().getBytes());
        kademliaService = new KademliaService("/dot/kad", hostId, isLocalEnabled);
        hostBuilder.addProtocols(List.of(new Ping(), new AutonatProtocol.Binding(), kademliaService.getDht()));

        host = hostBuilder.build();
        kademliaService.setHost(host);

        kademliaService.connectBootNodes(chainService.getGenesis().getBootNodes());
    }

    public static Network getInstance() {
        if (network != null) {
            return network;
        }
        throw new AssertionError("Network not initialized.");
    }

    public static Network initialize(ChainService chainService, HostConfig hostConfig) {
        if (network != null) {
            log.log(Level.WARNING, "Network module already initialized.");
            return network;
        }
        network = new Network(chainService, hostConfig);
        log.log(Level.INFO, "Initialized network module!");
        return network;
    }

    @Scheduled(fixedDelay = 10000)
    public void findPeers() throws InterruptedException {
        log.log(Level.INFO, "Searching for nodes...");
        try {
            kademliaService.findNewPeers();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error: " + e.getMessage());
        }
    }
}
