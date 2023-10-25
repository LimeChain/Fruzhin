package com.limechain.rpc.methods.sync;

import com.limechain.chain.ChainService;
import com.limechain.chain.ChainSpec;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Holds all business logic related to executing sync rpc method calls.
 * <p>
 * It should implement {@link SyncRPC}, however due to jsonrpc4j limitations
 * described in {@link com.limechain.rpc.methods.RPCMethodsImpl} it doesn't do that
 */
@Service
@AllArgsConstructor
public class SyncRPCImpl{

    private final ChainService chainService;

    /**
     * Returns the chain spec of the current sync state.
     * @param raw Whether to return the raw genesis or the decoded one (default: false).
     * @return The chain spec of the current sync state.
     */
    public ChainSpec syncStateGetSyncSpec(boolean raw) {
        //TODO: Consider should we send non raw genesis if raw is false
        //TODO: Local genesis should be updated with the Trie and saved
        return chainService.getGenesis();
    }
}
