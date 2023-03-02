package org.limechain.rpc.chain.events;

public class FollowEvent {
    public String event;

    // Start "initialized" event params
    public String finalizedBlockHash;
    public RuntimeInfo finalizedBlockRuntime;
    // End "initialized" event params

    // Start "newBlock" event params
    public String blockHash;
    public String parentBlockHash;
    public RuntimeInfo newRuntime;
    // End "newBlock" event params

    // Start "bestBlockChanged" event params
    public String bestBlockHash;
    // End "bestBlockChanged" event params

    // Start "finalized" event params
    public String[] finalizedBlockHashes;
    public String[] prunedBlockHashes;
    // End "finalized" event params
}
