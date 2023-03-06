package org.limechain.methods.chain;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

public interface ChainRPC {
    @JsonRpcMethod("chainHead_unstable_follow")
    void chainUnstableFollow ();

    @JsonRpcMethod("chainHead_unstable_unfollow")
    String chainUnstableUnfollow ();

    @JsonRpcMethod("chainHead_unstable_unpin")
    String chainUnstableUnpin ();

    @JsonRpcMethod("chainHead_unstable_storage")
    String chainUnstableStorage ();

    @JsonRpcMethod("chainHead_unstable_call")
    String chainUnstableCall ();

    @JsonRpcMethod("chainHead_unstable_stopCall")
    String chainUnstableStopCall ();


}
