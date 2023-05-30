package com.limechain.network;

import com.limechain.chain.Chain;
import com.limechain.chain.ChainService;
import com.limechain.config.HostConfig;
import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.blockannounce.BlockAnnounceService;
import com.limechain.network.protocol.lightclient.LightMessagesService;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.network.protocol.ping.Ping;
import com.limechain.network.protocol.sync.SyncService;
import com.limechain.network.protocol.warp.WarpSyncService;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.protocol.PingProtocol;
import lombok.Getter;
import lombok.extern.java.Log;
import org.peergos.HostBuilder;
import org.peergos.PeerAddresses;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.limechain.network.kad.KademliaService.REPLICATION;

/**
 * A Singleton Network class that handles all peer connections and Kademlia
 */
@Component
@Log
public class Network {
    private static final int HOST_PORT = 30333;
    private static Network network;
    private final String[] bootNodes;
    @Getter
    private final Map<Multihash, List<MultiAddress>> connections = new HashMap<>();
    public SyncService syncService;
    public LightMessagesService lightMessagesService;
    public KademliaService kademliaService;
    public BlockAnnounceService blockAnnounceService;
    public Ping ping;
    @Getter
    private Host host;
    private WarpSyncService warpSyncService;
    private PeerId currentSelectedPeer;

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
        this.initializeProtocols(chainService, hostConfig);
        this.bootNodes = chainService.getGenesis().getBootNodes();
        //TODO Remove hardcoded peer
        String selectedPeerId = "12D3KooWKer94o1REDPtAhjtYR4SdLehnSrN8PEhBnZm5NBoCrMC";
        this.currentSelectedPeer = new PeerId(Multihash.fromBase58(selectedPeerId).toBytes());
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

    private void initializeProtocols(ChainService chainService, HostConfig hostConfig) {
        boolean isLocalEnabled = hostConfig.getChain() == Chain.LOCAL;
        boolean clientMode = true;

        HostBuilder hostBuilder = new HostBuilder().generateIdentity().listenLocalhost(HOST_PORT);
        Multihash hostId = Multihash.deserialize(hostBuilder.getPeerId().getBytes());

        String pingProtocol = ProtocolUtils.getPingProtocol();
        //TODO: Add new protocolId format with genesis hash
        String chainId = chainService.getGenesis().getProtocolId();
        String legacyKadProtocolId = ProtocolUtils.getLegacyKadProtocol(chainId);
        String legacyWarpProtocolId = ProtocolUtils.getLegacyWarpSyncProtocol(chainId);
        String legacyLightProtocolId = ProtocolUtils.getLegacyLightMessageProtocol(chainId);
        String legacySyncProtocolId = ProtocolUtils.getLegacySyncProtocol(chainId);
        String legacyBlockAnnounceProtocolId = ProtocolUtils.getLegacyBlockAnnounceProtocol(chainId);

        kademliaService = new KademliaService(legacyKadProtocolId, hostId, isLocalEnabled, clientMode);
        lightMessagesService = new LightMessagesService(legacyLightProtocolId);
        warpSyncService = new WarpSyncService(legacyWarpProtocolId);
        syncService = new SyncService(legacySyncProtocolId);
        blockAnnounceService = new BlockAnnounceService(legacyBlockAnnounceProtocolId);
        ping = new Ping(pingProtocol, new PingProtocol());

        hostBuilder.addProtocols(
                List.of(
                        ping,
                        kademliaService.getProtocol(),
                        lightMessagesService.getProtocol(),
                        warpSyncService.getProtocol(),
                        syncService.getProtocol(),
                        blockAnnounceService.getProtocol()
                )
        );

        this.host = hostBuilder.build();

        kademliaService.host = host;
    }

    public void start() {
        log.log(Level.INFO, "Starting network module...");
        kademliaService.connectBootNodes(this.bootNodes);
        log.log(Level.INFO, "Started network module!");

    }

    public String getPeerId() {
        return this.host.getPeerId().toString();
    }

    public String[] getListenAddresses() {
        // TODO Bug: .listenAddresses() returns empty list
        return this.host.listenAddresses().stream().map(Multiaddr::toString).toArray(String[]::new);
    }

    public List<PeerAddresses> getPeers() {
        List<PeerAddresses> peers = new ArrayList<>();
        for (Map.Entry<Multihash, List<MultiAddress>> entry : connections.entrySet()) {
            PeerAddresses peer = new PeerAddresses(entry.getKey(), entry.getValue());
            peers.add(peer);
        }
        return peers;
    }

    /**
     * Periodically searches for new peers and connects to them
     * Logs the number of connected peers excluding boot nodes
     * By default Spring Boot uses a thread pool of size 1, so each call will be executed one at a time.
     */
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public synchronized void findPeers() {
        if (connections.size() >= REPLICATION) {
            log.log(Level.INFO,
                    "Connections have reached replication factor(" + REPLICATION + "). " +
                            "No need to search for new ones yet.");
            return;
        }

        log.log(Level.INFO, "Searching for peers...");
        Map<Multihash, List<MultiAddress>> newPeers = kademliaService.findNewPeers(connections);

        connections.putAll(newPeers);

        newPeers.forEach((peerId, addresses) -> log.log(Level.INFO, String.format("Found peer: " + peerId)));

        if (this.currentSelectedPeer == null && newPeers.size() > 0) {
            this.currentSelectedPeer = PeerId.fromBase58(newPeers.keySet().toArray()[0].toString());
        }

        log.log(Level.INFO, String.format("Connected peers: %s", connections.size()));
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public synchronized void pingPeers() {
        // TODO: This needs to by synchronized with the findPeers method
        if (connections.size() == 0) {
            log.log(Level.INFO, "No peers to ping.");
            return;
        }

        log.log(Level.INFO, "Pinging peers...");
        connections.forEach((peerId, addresses) -> {
            try {
                Long latency = ping.ping(host, host.getAddressBook(), PeerId.fromBase58(peerId.toBase58()));

                log.log(Level.INFO, String.format("Pinged peer: %s, latency %s ms", peerId, latency));
            } catch (Exception e) {
                log.log(Level.WARNING, String.format("Failed to ping peer: %s. " +
                        "Removing from active connections", peerId));
                connections.remove(peerId);

                if (this.currentSelectedPeer.toBase58().equals(peerId.toBase58())) {
                    this.currentSelectedPeer = null;
                    if (connections.size() > 0) {
                        this.currentSelectedPeer = PeerId.fromBase58(connections.keySet().toArray()[0].toString());
                    }
                }

            }
        });
    }

    public WarpSyncResponse makeWarpSyncRequest(String blockHash) {
        if (validatePeer()) return null;

        return this.warpSyncService.warpSync.warpSyncRequest(
                this.host,
                this.host.getAddressBook(),
                this.currentSelectedPeer,
                blockHash);
    }

    public void stop() {

    }

    public LightClientMessage.Response makeRemoteReadRequest(String blockHash, String[] keys) {
        if (validatePeer()) return null;

        return this.lightMessagesService.lightMessages.remoteReadRequest(
                this.host,
                this.host.getAddressBook(),
                this.currentSelectedPeer,
                blockHash,
                keys);

    }

    private boolean validatePeer() {
        if (this.currentSelectedPeer == null) {
            log.log(Level.WARNING, "No peer selected for warp sync request.");
            return true;
        }

        if (this.host.getAddressBook().get(this.currentSelectedPeer).join() == null) {
            log.log(Level.WARNING, "Peer not found in address book.");
            return true;
        }
        return false;
    }
}
