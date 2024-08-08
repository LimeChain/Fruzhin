package com.limechain.network.kad;

import com.limechain.network.kad.dto.Host;
import com.limechain.network.kad.dto.PeerId;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;

import java.util.Arrays;
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
    private static final Random RANDOM = new Random();

    @Setter
    private Host host;
    //    private List<PeerId> bootNodePeerIds;
    private int successfulBootNodes;


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
        startNetwork(bootNodes);
        Object peer = getPeerId();
        while (peer.toString().equalsIgnoreCase("undefined")) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            peer = getPeerId();
        }
        String peerIdStr = peer.toString();
        byte[] privateKey = getPeerPrivateKey();
        byte[] publicKey = getPeerPublicKey();

        PeerId peerId = new PeerId(privateKey, publicKey, peerIdStr);
        this.host = new Host(peerId);

        successfulBootNodes = getPeerStoreSize();

        if (successfulBootNodes > 0)
            log.log(Level.INFO, "Successfully connected to " + successfulBootNodes + " boot nodes");
        else log.log(Level.SEVERE, "Failed to connect to boot nodes");
        return successfulBootNodes;
    }

    @JSBody(params = {"bootNodes"}, script = "start(bootNodes)")
    @Async
    public static native void startNetwork(String[] bootNodes);

    @JSBody(script = "return getPeerId()")
    public static native Object getPeerId();
    @JSBody(script = "return libp.peerId.privateKey")
    public static native byte[] getPeerPrivateKey();
    @JSBody(script = "return libp.peerId.publicKey")
    public static native byte[] getPeerPublicKey();
    @JSBody(script = "return libp.peerStore.store.datastore.data.size")
    public static native int getPeerStoreSize();


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