package com.limechain.rpc.methods.sync;

import com.limechain.chain.ChainService;
import com.limechain.chain.spec.RawChainSpec;
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
    public RawChainSpec syncStateGetSyncSpec(boolean raw) {
        //Currently we are not taking the raw boolean in consideration, because in the specificaiton
        //it is not defined what should be returned if raw is true or false
        //The Parity Polkadot implementation returns the raw genesis nevertheless if raw is true or false and does not
        //take it into consideration at all. The boolean is reserved for future updates in the specification
        //Gossamer has decided to return the raw genesis if raw is true and the decoded one if raw is false - we might
        //do that as well in the future when we have working Trie and dynamic genesis sync spec
        //TODO: Consider should we send non raw genesis if raw is false
        //TODO: Local genesis should be updated with the Trie and saved
        return chainService.getChainSpec().getRawChainSpec();
    }
}
