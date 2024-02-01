package com.limechain.client;

import com.google.common.primitives.Bytes;
import com.limechain.chain.ChainService;
import com.limechain.cli.CliArguments;
import com.limechain.chain.spec.Genesis;
import com.limechain.network.Network;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.StateVersion;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.BlockStateHelper;
import com.limechain.sync.fullsync.FullSyncMachine;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.trie.TrieStructureFactory;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.InsertTrieBuilder;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.database.TrieBuildException;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.node.InsertTrieNode;
import com.limechain.trie.structure.node.NodeChildData;
import com.limechain.trie.structure.node.TrieNodeData;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;

@Log
public class FullNode implements HostNode {
    private static final String TRIE_NODE_PREFIX = "tn:";
    private static final String TRIE_NODE_STORAGE_PREFIX = "tns:";
    private static final String TRIE_NODE_CHILD_PREFIX = "tnc:";

    /**
     * Starts the light client by instantiating all dependencies and services
     *
     * @implNote the RpcApp is assumed to have been started before starting the client,
     * as it relies on the application context
     */
    @SneakyThrows
    public void start() {
        //Initialize state

        // TODO: Is there a better way to decide whether we've got any database written?
        var db = AppBean.getBean(KVRepository.class); //presume this works
        // if: database exists and has some persisted storage
        if (db != null && db.find(new BlockStateHelper().headerHashKey(BigInteger.ZERO)).isPresent()) {
            // do nothing?
        } else {
            Genesis genesisStorage = loadGenesisStorage();

            // TODO: Next up, manage to fetch the runtime version
            StateVersion stateVersion = StateVersion.V0;

            TrieStructure<NodeData> trie = TrieStructureFactory.buildFromKVPs(genesisStorage.getTop(), stateVersion);
            List<InsertTrieNode> dbSerializedTrieNodes = new InsertTrieBuilder(trie).build();
            insertStorage(db, dbSerializedTrieNodes, InsertTrieBuilder.STATE_VERSION); //Todo: calculate state version
        }

        // Start network
        final Network network = AppBean.getBean(Network.class);
        network.start();

        // Wait for peers
        while (true) {
            if (!network.getKademliaService().getBootNodePeerIds().isEmpty()) {
                if (network.getKademliaService().getSuccessfulBootNodes() > 0) {
                    break;
                }
                network.updateCurrentSelectedPeer();
            }

            log.log(Level.INFO, "Waiting for peer connection...");
            Thread.sleep(10000); // TODO: Maybe extract this number into an application.property as it's duplicated
        }

        // Start syncing
        log.log(Level.INFO, "Node successfully connected to a peer! Sync can start!");

        CliArguments args = AppBean.getBean(CliArguments.class);
        switch (args.syncMode()){
            case FULL -> AppBean.getBean(FullSyncMachine.class).start();
            case WARP -> AppBean.getBean(WarpSyncMachine.class).start();
            default -> throw new IllegalStateException("Unexpected value: " + args.syncMode());
        }
    }

    private static Genesis loadGenesisStorage() {
        return AppBean.getBean(ChainService.class).getChainSpec().getGenesis();
    }

    /**
     * Inserts trie nodes into the key-value repository.
     *
     * @param db              The key-value repository where trie nodes are to be stored.
     * @param insertTrieNodes The list of trie nodes to be inserted.
     * @param entriesVersion  The version number of the trie entries.
     */
    private void insertStorage(KVRepository<String, Object> db, List<InsertTrieNode> insertTrieNodes,
                               int entriesVersion) {
        try {
            for (InsertTrieNode trieNode : insertTrieNodes) {
                insertTrieNode(db, trieNode);
                insertTrieNodeStorage(db, trieNode, entriesVersion);
                insertChildren(db, trieNode);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to insert trie structure to db storage", e);
        }
    }

    private void insertTrieNode(KVRepository<String, Object> db, InsertTrieNode trieNode) {
        if (trieNode.merkleValue() == null) {
            throw new TrieBuildException("Missing merkle value");
        }

        String key = TRIE_NODE_PREFIX + new String(trieNode.merkleValue());
        byte[] value = Bytes.toArray(
                trieNode.partialKeyNibbles().stream()
                        .map(Nibble::asByte)
                        .toList());

        db.save(key, value);
    }

    private void insertTrieNodeStorage(KVRepository<String, Object> db, InsertTrieNode trieNode, int version) {
        String key = TRIE_NODE_STORAGE_PREFIX + new String(trieNode.merkleValue());
        TrieNodeData storageValue = new TrieNodeData(
                trieNode.isReferenceValue() ? null : trieNode.storageValue(),
                trieNode.isReferenceValue() ? trieNode.storageValue() : null,
                (byte) version);

        db.save(key, storageValue);
    }

    /**
     * Inserts the children of a given trie node into
     * the key-value repository.
     *
     * @param db       The key-value repository where trie node children are to be stored.
     * @param trieNode The trie node whose children are to be inserted.
     */
    private void insertChildren(KVRepository<String, Object> db, InsertTrieNode trieNode) {
        String key = TRIE_NODE_CHILD_PREFIX + new String(trieNode.merkleValue());
        List<byte[]> childrenMerkleValues = trieNode.childrenMerkleValues();

        for (int childNum = 0; childNum < childrenMerkleValues.size(); childNum++) {
            byte[] child = childrenMerkleValues.get(childNum);
            NodeChildData nodeChildData = new NodeChildData(childNum, child);

            db.save(key, nodeChildData);
        }
    }
}
