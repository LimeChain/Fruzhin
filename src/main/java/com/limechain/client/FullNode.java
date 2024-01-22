package com.limechain.client;

import com.limechain.chain.ChainService;
import com.limechain.network.Network;
import com.limechain.rpc.server.AppBean;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.BlockStateHelper;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.trie.structure.InsertTrieBuilder;
import com.limechain.trie.structure.node.InsertTrieNode;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
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
            List<InsertTrieNode> trie = new InsertTrieBuilder()
                    .initializeTrieStructure(loadGenesisStorage())
                    .build();

            insertStorage(db, trie, (byte) 0); //Todo: calculate state versoin
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

    private Map<String, String> loadGenesisStorage() {
        var genesisStorageRaw = AppBean.getBean(ChainService.class).getGenesis().getGenesis().getRaw();
        return genesisStorageRaw.get("top");
    }

    /**
     * Inserts trie nodes into the key-value repository.
     *
     * @param db The key-value repository where trie nodes are to be stored.
     * @param insertTrieNodes The list of trie nodes to be inserted.
     * @param entriesVersion The version number of the trie entries.
     */
    private void insertStorage(KVRepository<String, Object> db, List<InsertTrieNode> insertTrieNodes, byte entriesVersion) {
        try {
            for (InsertTrieNode trieNode : insertTrieNodes) {
                //Insert tn(trie node) to db
                db.save("tn:" + new String(trieNode.getMerkleValue()), trieNode.getPartialKeyNibbles());

                // Handle storage value
                final InsertTrieNode.InsertStorageValue storageValue = trieNode.getStorageValue();
                final TrieNodeData tnsValue = new TrieNodeData(
                        storageValue.isReferencesMerkleValue() ? null : storageValue.getValue(),
                        storageValue.isReferencesMerkleValue() ? storageValue.getValue() : null,
                        entriesVersion);

                //Insert tns(trie node storage) to db
                db.save("tns:" + new String(trieNode.getMerkleValue()), tnsValue);

                // Insert children
                insertChildren(db, trieNode);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to insert trie structure to db storage", e);
        }
    }

    /**
     * Inserts the children of a given trie node into
     the key-value repository.

     @param db The key-value repository where trie node children are to be stored.
     @param trieNode The trie node whose children are to be inserted.
     */
    private void insertChildren(KVRepository<String, Object> db, InsertTrieNode trieNode)  {
        for (int childNum = 0; childNum < trieNode.getChildrenMerkleValues().size(); childNum++) {
            byte[] child = trieNode.getChildrenMerkleValues().get(childNum);
            NodeChildData nodeChildData = new NodeChildData(childNum, child);
            //Insert tnc(trie node child) to db
            db.save("tnc:" + new String(trieNode.getMerkleValue()), nodeChildData);
        }
    }

}
