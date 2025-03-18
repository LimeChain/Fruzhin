package com.limechain.consensus.beefy.dto;

import com.limechain.network.protocol.beefy.messages.vote.VoteMessage;
import lombok.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class BeefyRound {
    //TODO: find an appropriate key for the 33 byte array
    Map<byte[], VoteMessage> signedVotes = new ConcurrentHashMap<>();
}
