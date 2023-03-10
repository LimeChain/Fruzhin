package com.limechain.rpc.methods.chain.events;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class FollowEvent {
    private String event;

    // Start "initialized" event params
    private String finalizedBlockHash;
    private RuntimeInfo finalizedBlockRuntime;
    // End "initialized" event params

    // Start "newBlock" event params
    private String blockHash;
    private String parentBlockHash;
    private RuntimeInfo newRuntime;
    // End "newBlock" event params

    // Start "bestBlockChanged" event params
    private String bestBlockHash;
    // End "bestBlockChanged" event params

    // Start "finalized" event params
    private String[] finalizedBlockHashes;
    private String[] prunedBlockHashes;
    // End "finalized" event params
}
