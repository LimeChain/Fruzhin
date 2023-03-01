package org.limechain.rpc.chain;

import org.springframework.stereotype.Service;

@Service
public class ChainRPCImpl {
    private final String NodeEndpoint = "";

    public String chainUnstableFollow () {
        return "test";
    }

    public String chainUnstableUnfollow () {
        return null;
    }

    public String chainUnstableUnpin () {
        return null;
    }

    public String chainUnstableStorage () {
        return null;
    }

    public String chainUnstableCall () {
        return null;
    }

    public String chainUnstableStopCall () {
        return null;
    }
}
