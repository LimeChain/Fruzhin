package com.limechain.client;

import com.google.protobuf.ByteString;
import com.limechain.cli.CliArguments;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.RuntimeBuilder;
import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.BlockState;
import com.limechain.storage.block.BlockStateHelper;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.sync.fullsync.FullSyncMachine;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.InsertTrieBuilder;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.node.InsertTrieNode;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class FullNode implements HostNode {

    /**
     * Inserts trie nodes into the key-value repository.
     *
     * @param db              The key-value repository.
     * @param insertTrieNodes The list of trie nodes to be inserted.
     * @param stateVersion    The version number of the trie entries.
     */
    private static void saveTrieNodes(final KVRepository<String, Object> db, final List<InsertTrieNode> insertTrieNodes,
                                      final StateVersion stateVersion) {
        TrieStorage trieStorage = TrieStorage.getInstance();
        try {
            for (InsertTrieNode trieNode : insertTrieNodes) {
                trieStorage.insertTrieNodeStorage(db, trieNode, stateVersion);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to insert trie structure to db storage", e);
        }
    }

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
        KVRepository<String, Object> db = AppBean.getBean(KVRepository.class); //presume this works

        // if: database has some persisted storage
        if (db == null) {
            throw new IllegalStateException("Database is not initialized");
        }
        TrieStorage.getInstance().initialize(db);//Initialize TrieStorage (BlockState is prerequisite)
        if (db.find(new BlockStateHelper().headerHashKey(BigInteger.ZERO)).isPresent()) {
            BlockState.getInstance().initialize(db);//Initialize BlockState from already existing data
        } else {
            GenesisBlockHash genesisBlockHash = AppBean.getBean(GenesisBlockHash.class);
            BlockState.getInstance().initialize(db,
                    genesisBlockHash.getGenesisBlockHeader()); //Initialize BlockState from genesis block

            StateVersion stateVersion = new RuntimeBuilder().buildRuntime(
                    genesisBlockHash.getGenesisStorage().get(ByteString.copyFrom(":code".getBytes())).toByteArray()
            ).getVersion().getStateVersion();

            TrieStructure<NodeData> trie = genesisBlockHash.getGenesisTrie();
            List<InsertTrieNode> dbSerializedTrieNodes = new InsertTrieBuilder(trie).build();
            saveTrieNodes(db, dbSerializedTrieNodes, stateVersion);
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
        switch (args.syncMode()) {
            case FULL -> AppBean.getBean(FullSyncMachine.class).start();
            case WARP -> AppBean.getBean(WarpSyncMachine.class).start();
            default -> throw new IllegalStateException("Unexpected value: " + args.syncMode());
        }
    }

}
