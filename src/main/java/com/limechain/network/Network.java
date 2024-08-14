package com.limechain.network;

import com.limechain.chain.Chain;
import com.limechain.chain.ChainService;
import com.limechain.config.HostConfig;
import com.limechain.network.kad.KademliaService;
import com.limechain.rpc.server.AppBean;
import com.limechain.sync.warpsync.WarpSyncState;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.Random;
import java.util.logging.Level;

import static com.limechain.network.kad.KademliaService.REPLICATION;

/**
 * A Network class that handles all peer connections and Kademlia
 */
@Log
public class Network {
    private static final Random RANDOM = new Random();
    @Getter
    private final Chain chain;
    @Getter
    private final String[] bootNodes;
    //    private final ConnectionManager connectionManager;
    @Getter
    private KademliaService kademliaService;
    private boolean started = false;
    private int bootPeerIndex = 0;

    /**
     * Initializes a host for the peer connection,
     * Initializes the Kademlia service
     * Manages if nodes running locally are going to be allowed
     * Connects Kademlia to boot nodes
     *
     * @param chainService chain specification information containing boot nodes
     * @param hostConfig   host configuration containing current network
     */
    public Network(ChainService chainService, HostConfig hostConfig) {
        this.bootNodes = chainService.getChainSpec().getBootNodes();
        this.chain = hostConfig.getChain();
//        this.connectionManager = ConnectionManager.getInstance();
        this.initializeProtocols(chainService, hostConfig);
    }

    private void initializeProtocols(ChainService chainService,
                                     HostConfig hostConfig) {

//
//        String chainId = chainService.getChainSpec().getProtocolId();
//        String warpProtocolId = ProtocolUtils.getWarpSyncProtocol(chainId);
//        String lightProtocolId = ProtocolUtils.getLightMessageProtocol(chainId);
//        String blockAnnounceProtocolId = ProtocolUtils.getBlockAnnounceProtocol(chainId);
//        String grandpaProtocolId = ProtocolUtils.getGrandpaProtocol(chainId);

        kademliaService = new KademliaService();
    }

//    private Ed25519PrivateKey loadPrivateKeyFromDB(KVRepository<String, Object> repository) {
//        Ed25519PrivateKey privateKey;
//
//        Optional<Object> peerIdKeyBytes = repository.find(DBConstants.PEER_ID);
//        if (peerIdKeyBytes.isPresent()) {
//            privateKey = Ed25519Utils.loadPrivateKey((byte[]) peerIdKeyBytes.get());
//            log.log(Level.INFO, "PeerId loaded from database!");
//        } else {
//            privateKey = Ed25519Utils.generateKeyPair();
//            repository.save(DBConstants.PEER_ID, privateKey.raw());
//            log.log(Level.INFO, "Generated new peerId!");
//        }
//        return privateKey;
//    }

    public void start() {
        log.log(Level.INFO, "Starting network module...");

        kademliaService.connectBootNodes(this.bootNodes);
        started = true;
        log.log(Level.INFO, "Started network module!");
    }

    public void stop() {
        log.log(Level.INFO, "Stopping network module...");
        started = false;
//        connectionManager.removeAllPeers();
//        host.stop();
        log.log(Level.INFO, "Stopped network module!");
    }

    public boolean updateCurrentSelectedPeerWithNextBootnode() {
//        if (bootPeerIndex > kademliaService.getBootNodePeerIds().size())
//            return false;
//        this.currentSelectedPeer = this.kademliaService.getBootNodePeerIds().get(bootPeerIndex);
        bootPeerIndex++;
        return true;
    }

    public boolean updateCurrentSelectedPeerWithBootnode(int index) {
//        if (index >= 0 && index < this.kademliaService.getBootNodePeerIds().size()) {
//            this.currentSelectedPeer = this.kademliaService.getBootNodePeerIds().get(index);
//            return true;
//        }
        return false;
    }

    public void updateCurrentSelectedPeer() {
//        if (connectionManager.getPeerIds().isEmpty()) return;
//        this.currentSelectedPeer = connectionManager.getPeerIds().stream()
//                .skip(RANDOM.nextInt(connectionManager.getPeerIds().size())).findAny().orElse(null);
    }

//    public String getPeerId() {
//        return this.host.getPeerId().toString();
//    }

    public String[] getListenAddresses() {
        // TODO Bug: .listenAddresses() returns empty list
//        return this.host.listenAddresses().stream().map(Multiaddr::toString).toArray(String[]::new);
        return null;
    }

    public int getPeersCount() {
        return 0;//connectionManager.getPeerIds().size();
    }

    /**
     * Periodically searches for new peers and connects to them
     * Logs the number of connected peers excluding boot nodes
     * By default Spring Boot uses a thread pool of size 1, so each call will be executed one at a time.
     */
//    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void findPeers() {
        if (!started) {
            return;
        }
        if (getPeersCount() >= REPLICATION) {
            log.log(Level.INFO,
                "Connections have reached replication factor(" + REPLICATION + "). " +
                    "No need to search for new ones yet.");
            return;
        }

        log.log(Level.INFO, "Searching for peers...");
        kademliaService.findNewPeers();

//        if (this.currentSelectedPeer == null) {
//            updateCurrentSelectedPeer();
//        }

        log.log(Level.INFO, String.format("Connected peers: %s", getPeersCount()));
    }

    //    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void pingPeers() {
        // TODO: This needs to by synchronized with the findPeers method
        if (getPeersCount() == 0) {
            log.log(Level.INFO, "No peers to ping.");
            return;
        }

        log.log(Level.INFO, "Pinging peers...");
//        connectionManager.getPeerIds().forEach(this::ping);
    }

//    private void ping(PeerId peerId) {
//        try {
//            Long latency = ping.ping(host, host.getAddressBook(), peerId);
//            log.log(Level.INFO, String.format("Pinged peer: %s, latency %s ms", peerId, latency));
//        } catch (Exception e) {
//            log.log(Level.WARNING, String.format("Failed to ping peer: %s. Removing from active connections", peerId));
//            if (this.currentSelectedPeer.equals(peerId)) {
//                updateCurrentSelectedPeer();
//            }
//        }
//    }

//    public BlockResponse syncBlock(PeerId peerId, BigInteger lastBlockNumber) {
//        this.currentSelectedPeer = peerId;
//        // TODO: fields, hash, direction and maxBlocks values not verified
//        // TODO: when debugging could not get a value returned
//        return this.makeBlockRequest(
//                new BlockRequestDto(19, null, lastBlockNumber.intValue(), Direction.Ascending, 1));
//    }
//
//    public BlockResponse makeBlockRequest(BlockRequestDto blockRequestDto) {
//        return syncService.getProtocol().remoteBlockRequest(
//                this.host,
//                this.currentSelectedPeer,
//                blockRequestDto
//        );
//    }
//
//    public SyncMessage.StateResponse makeStateRequest(String blockHash, ByteString after) {
//        return stateService.getProtocol().remoteStateRequest(
//                this.host,
//                this.currentSelectedPeer,
//                blockHash,
//                after
//        );
//    }
//
//    public WarpSyncResponse makeWarpSyncRequest(String blockHash) {
//        if (isPeerInvalid()) return null;
//
//        return this.warpSyncService.getProtocol().warpSyncRequest(
//                this.host,
//                this.currentSelectedPeer,
//                blockHash);
//    }
//
//    public LightClientMessage.Response makeRemoteReadRequest(String blockHash, String[] keys) {
//        if (isPeerInvalid()) return null;
//
//        return this.lightMessagesService.getProtocol().remoteReadRequest(
//                this.host,
//                this.currentSelectedPeer,
//                blockHash,
//                keys);
//
//    }
//
//    private boolean isPeerInvalid() {
//        if (this.currentSelectedPeer == null) {
//            log.log(Level.WARNING, "No peer selected for warp sync request.");
//            return true;
//        }
//
//        if (this.host.getAddressBook().get(this.currentSelectedPeer).join() == null) {
//            log.log(Level.WARNING, "Peer not found in address book.");
//            return true;
//        }
//        return false;
//    }
//
//    public void handshakeBootNodes() {
//        kademliaService.getBootNodePeerIds()
//                .stream()
//                .distinct()
//                .forEach(this::sendGrandpaHandshake);
//    }
//
//    private void sendGrandpaHandshake(PeerId peerId) {
//        //TODO:
//        // when using threads we connect to more than 10 peers, but have some unhandled exceptions,
//        // without them we connect to only 2 peers
//        new Thread(() ->
//                blockAnnounceService.sendHandshake(this.host, this.host.getAddressBook(), peerId)
//        ).start();
//    }

    //    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void sendNeighbourMessages() {
        if (!AppBean.getBean(WarpSyncState.class).isWarpSyncFinished()) {
            return;
        }
//        connectionManager.getPeerIds().forEach(peerId -> grandpaService.sendNeighbourMessage(this.host, peerId));
    }
//
//    public void sendNeighbourMessage(PeerId peerId) {
//        grandpaService.sendNeighbourMessage(this.host, peerId);
//    }
}
