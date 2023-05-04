package com.limechain.rpc.methods.system;

import com.limechain.chain.ChainService;
import com.limechain.config.SystemInfo;
import com.limechain.network.Network;
import com.limechain.sync.Sync;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.util.Map.entry;

/**
 * Holds all business logic related to executing system rpc method calls.
 * <p>
 * It should implement {@link SystemRPC}, however due to jsonrpc4j limitations
 * described in {@link com.limechain.rpc.methods.RPCMethodsImpl} it doesn't do that
 */
@Service
@AllArgsConstructor
public class SystemRPCImpl {
    /**
     * Reference to services used when executing business logic
     */
    private final ChainService chainService;
    private final SystemInfo systemInfo;
    private final Network network;
    private final Sync sync;

    /**
     * Gets the node's implementation name.
     */
    public String systemName() {
        return this.systemInfo.getHostName();
    }

    /**
     * Get the node implementation's version. Should be a semantic versioning string.
     */
    public String systemVersion() {
        return this.systemInfo.getHostVersion();
    }

    /**
     * Get the chain's type. Given as a string identifier.
     */
    public String systemChain() {
        return this.chainService.getGenesis().getName();
    }

    /**
     * Get the chain's type.
     */
    public String systemChainType() {
        return this.chainService.getGenesis().getChainType();
    }

    /**
     * Get a custom set of properties as a JSON object, defined in the chain specification.
     */
    public Map<String, Object> systemProperties() {
        return this.chainService.getGenesis().getProperties();
    }

    /**
     * Returns the roles the node is running as.
     */
    public String[] systemNodeRoles() {
        return new String[]{this.systemInfo.getRole()};
    }

    /**
     * Return health status of the node.
     * <p>
     * Node is considered healthy if it is:
     * <li>Connected to some peers (unless running in dev mode).</li>
     * <li>Not performing a major sync.</li>
     */
    public Map<String, Object> systemHealth() {
        return Map.ofEntries(
                entry("isSyncing", this.sync.isSyncing()),
                entry("peers", this.network.getPeers().size()),
                entry("shouldHavePeers", chainService.isChainLive())
        );
    }

    /**
     * Returns the base58-encoded PeerId of the node.
     */
    public String systemLocalPeerId() {
        return network.getPeerId();
    }

    /**
     * Returns the libp2p multiaddresses that the local node is listening on.
     * <p>
     * The addresses include a trailing /p2p/ with the local PeerId, and are thus
     * suitable to be passed to system_addReservedPeer or as a bootnode address for example.
     */
    public String[] systemLocalListenAddress() {
        return this.network.getListenAddresses();
    }

    /**
     * Returns currently connected peers.
     */
    // TODO: Implement in M2.
    public String[] systemSystemPeers() {
        return new String[0];
    }

    /**
     * Adds a reserved peer. The string parameter should encode a p2p multiaddr.
     *
     * @param peerId peerId to add
     */
    // TODO: Implement in Mx.
    public void systemAddReservedPeer(String peerId) {
    }

    /**
     * Remove a reserved peer. The string should encode only the PeerId
     * e.g. QmSk5HQbn6LhUwDiNMseVUjuRYhEtYj4aUZ6WfWoGURpdV.
     *
     * @param peerId peerId to remove
     */
    // TODO: Implement in M2.
    public void systemRemoveReservedPeer(String peerId) {
    }

    /**
     * Returns the state of the syncing of the node.
     */
    // TODO: Implement in Mx.
    public Map<String, Object> systemSyncState() {
        return null;
    }

    /**
     * Returns the next valid index (aka. nonce) for given account.
     * <p>
     * This method takes into consideration all pending transactions
     * currently in the pool and if no transactions are found in the pool
     * it fallbacks to query the index from the runtime (aka. state nonce).
     *
     * @param accountAddress the address of the account
     */
    // TODO: Implement in Mx.
    public String systemAccountNextIndex(String accountAddress) {
        return null;
    }

    /**
     * Dry run an extrinsic. Returns a SCALE encoded ApplyExtrinsicResult.
     *
     * @param extrinsic the raw, SCALE encoded extrinsic
     * @param blockHash the block hash indicating the state. Null implies the current state
     */
    // TODO: Implement in Mx.
    public String systemDryRun(String extrinsic, String blockHash) {
        return null;
    }

}
