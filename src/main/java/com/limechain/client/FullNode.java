package com.limechain.client;

import com.limechain.network.Network;
import com.limechain.rpc.server.AppBean;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.BlockStateHelper;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.node.InsertTrieNode;
import com.limechain.trie.structure.node.TrieNodeIndex;
import com.limechain.trie.structure.node.handle.NodeHandle;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class FullNode implements HostNode {
    /**
     * Starts the light client by instantiating all dependencies and services
     *
     * @implNote the RpcApp is assumed to have been started before starting the client,
     * as it relies on the application context
     */
    @SneakyThrows
    public void start() {
        //Initialize state

        // TODO: Is there a better way to do this?
        var db = AppBean.getBean(KVRepository.class); //presume this works
        // if: database exists and has some persisted storage
        if (db != null && db.find(new BlockStateHelper().headerHashKey(BigInteger.ZERO)).isPresent()) {
            // do nothing?
        } else {
            // do the initial one-time population of the database with the genesis storage
            TrieStructure<Pair<Optional<byte[]>, Optional<byte[]>>> trieStructure = initializeDatabaseStorageTrie();
            buildTrieNodesIterator(trieStructure);
        }

        // Start network
        final Network network = AppBean.getBean(Network.class);
        network.start();

        // Wait for peers
        while (true) {
            if (!network.kademliaService.getBootNodePeerIds().isEmpty()) {
                if (network.kademliaService.getSuccessfulBootNodes() > 0) {
                    break;
                }
                network.updateCurrentSelectedPeer();
            }

            log.log(Level.INFO, "Waiting for peer connection...");
            Thread.sleep(10000); // TODO: Maybe extract this number into an application.property as it's duplicated
        }

        // Start syncing
        log.log(Level.INFO, "Node successfully connected to a peer! Sync can start!");
        AppBean.getBean(WarpSyncMachine.class).start();
    }

    /**
     * Builds a list of {@link InsertTrieNode} objects representing the nodes in a trie structure.
     * Each trie node is constructed with its storage value, merkle value, children's merkle values,
     * and partial key nibbles.
     *
     * @param trieStructure The trie structure containing the nodes. This structure should be
     *                      a {@link TrieStructure} with user data of type {@link Pair<Optional<byte[]>, byte[]>},
     *                      where the first element of the pair is an optional storage value (byte array),
     *                      and the second element is the merkle value (byte array).
     * @return A list of {@link InsertTrieNode} objects representing the nodes in the given trie structure.
     * @throws IllegalStateException if the user data in the trie structure is empty or null, which
     *                               indicates an invalid state for the trie nodes.
     */
    public List<InsertTrieNode> buildTrieNodesIterator(
            TrieStructure<Pair<Optional<byte[]>, Optional<byte[]>>> trieStructure) {
        List<InsertTrieNode> trieNodesIterator = new ArrayList<>();

        for (TrieNodeIndex nodeIndex : trieStructure) {
            Pair<Optional<byte[]>, Optional<byte[]>> userData = trieStructure.getUserDataAtIndex(nodeIndex.value());
            if (userData.getValue1().isEmpty()) {
                throw new IllegalStateException("Merkle value should not be empty!");
            }
            NodeHandle<Pair<Optional<byte[]>, Optional<byte[]>>> nodeHandle = trieStructure.nodeAtIndex(nodeIndex);

            byte[] storageValue = userData.getValue0().orElse(null);
            byte[] merkleValue = userData.getValue1().get();

            byte[] merkleValueCopy = merkleValue.clone();
            List<byte[]> childrenMerkleValues = new ArrayList<>();
            List<Nibble> partialKeyNibbles = new ArrayList<>(nodeHandle.getPartialKey());

            for (Nibble n = Nibble.fromInt(0); n.toByte() < 16; n = Nibble.fromInt(n.toByte() + 1)) {
                Optional<NodeHandle<Pair<Optional<byte[]>, Optional<byte[]>>>> childHandle = nodeHandle.getChild(n);
                childHandle.ifPresent(handle -> {
                    Pair<Optional<byte[]>, Optional<byte[]>> child = handle.getUserData();
                    if (child != null && child.getValue1().isPresent()) {
                        childrenMerkleValues.add(child.getValue1().get().clone());
                    }
                });
            }

            trieNodesIterator.add(new InsertTrieNode(
                    storageValue,
                    merkleValueCopy,
                    childrenMerkleValues,
                    partialKeyNibbles
            ));
        }

        return trieNodesIterator;
    }

    TrieStructure<Pair<Optional<byte[]>, Optional<byte[]>>> initializeDatabaseStorageTrie() {
        TrieStructure<Pair<Optional<byte[]>, Optional<byte[]>>> trieStructure = null;
        // TODO: Build trie structure from genesis

        return trieStructure;
    }
}
