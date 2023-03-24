package com.limechain.rpc.subscriptions.chainhead;

/**
 * Interface for chainHead rpc methods family that the light client <b>must</b> implement
 */
public interface ChainHeadRpc {

    /**
     * Initiates a subscription which lets the JSON-RPC client track the state of the head of the chain:
     * the finalized, non-finalized, and best blocks.
     *
     * @param runtimeUpdates whether the events should report changes to the runtime
     * @see <a href="https://paritytech.github.io/json-rpc-interface-spec/api/chainHead_unstable_follow.html">
     * chainHead_unstable_follow</a>
     */
    void chainUnstableFollow(boolean runtimeUpdates);

    /**
     * Stops a subscription started with {@link #chainUnstableFollow(boolean)}
     *
     * @param subscriptionId An opaque string that was returned by {@link #chainUnstableFollow(boolean)}
     * @see <a href="https://paritytech.github.io/json-rpc-interface-spec/api/chainHead_unstable_unfollow.html">
     * chainHead_unstable_unfollow</a>
     */
    void chainUnstableUnfollow(String subscriptionId);

    /**
     * Unpin a block from the Host's memory
     *
     * @param subscriptionId An opaque string that was returned by {@link #chainUnstableFollow(boolean)}
     * @param blockHash      String containing the hexadecimal-encoded hash of the header of the block to unpin
     * @see <a href="https://paritytech.github.io/json-rpc-interface-spec/api/chainHead_unstable_unpin">
     * chainHead_unstable_unpin</a>
     * @see <a href="https://paritytech.github.io/json-rpc-interface-spec/api/chainHead_unstable_follow.html#pinning">
     * specification</a> for more information about pinning
     */
    void chainUnstableUnpin(String subscriptionId, String blockHash);

    /**
     * Invokes the entry point of the runtime of the given block using the storage of the given block
     *
     * @param subscriptionId An opaque string that was returned by {@link #chainUnstableFollow(boolean)}
     * @param blockHash      String containing the hexadecimal-encoded hash of the
     *                       header of the block to make the call against
     * @param function       Name of the runtime entry point to call as a string
     * @param callParameters Hexadecimal-encoded SCALE-encoded value to pass as input to the runtime function
     * @see <a href="https://paritytech.github.io/json-rpc-interface-spec/api/chainHead_unstable_call.html">
     * chainHead_unstable_call</a>
     */
    void chainUnstableCall(String subscriptionId, String blockHash, String function, String callParameters);

    /**
     * Obtains the value of the entry with the given key from the storage.
     *
     * @param subscriptionId An opaque string that was returned by {@link #chainUnstableFollow(boolean)}
     * @param blockHash      String containing an hexadecimal-encoded hash of the
     *                       header of the block whose storage to fetch
     * @param key            String containing the hexadecimal-encoded key to fetch in the storage
     * @see <a href="https://paritytech.github.io/json-rpc-interface-spec/api/chainHead_unstable_storage.html">
     * chainhead_unstable_storage</a>
     */
    void chainUnstableStorage(String subscriptionId, String blockHash, String key);

    /**
     * Stops a call started with chainHead_unstable_call. If the call was still in progress, this interrupts it.
     * If the call was already finished, this call has no effect.
     *
     * @param subscriptionId An opaque string that was returned
     *                       by {@link #chainUnstableCall(String, String, String, String)}}
     * @see <a href="https://paritytech.github.io/json-rpc-interface-spec/api/chainHead_unstable_stopCall.html">
     * chainHead_unstable_stopCall</a>
     */
    void chainUnstableStopCall(String subscriptionId);
}
