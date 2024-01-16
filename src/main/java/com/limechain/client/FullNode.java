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
}
