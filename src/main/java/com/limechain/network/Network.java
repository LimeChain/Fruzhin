package com.limechain.network;

import com.limechain.chain.Chain;
import com.limechain.chain.ChainService;
import com.limechain.chain.ChainSpec;
import com.limechain.config.HostConfig;
import com.limechain.network.kad.KademliaService;
import com.offbynull.kademlia.Id;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.protocol.Ping;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.peergos.HostBuilder;
import org.peergos.protocol.autonat.AutonatProtocol;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
        hostBuilder = (new HostBuilder()).generateIdentity().listenLocalhost(1001);
        Multihash hostId = Multihash.deserialize(hostBuilder.getPeerId().getBytes());
        boolean isLocalEnabled = hostConfig.getChain() == Chain.LOCAL;
        kademliaService = new KademliaService("/dot/kad", List.of(chainService.getGenesis().getBootNodes()), hostId, isLocalEnabled);
        hostBuilder.addProtocols(List.of(new Ping(), new AutonatProtocol.Binding(), kademliaService.getDht()));
        host = hostBuilder.build();

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
        log.log(Level.INFO, "Finding peers");
        try {
            kademliaService.findNewPeers();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error: " + e.getMessage());
        }
    }
}
