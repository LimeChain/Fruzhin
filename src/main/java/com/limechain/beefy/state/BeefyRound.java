package com.limechain.beefy.state;

import com.limechain.beefy.dto.VoteMessage;
import lombok.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class BeefyRound {
    //mapper key is authority public key
    Map<byte[], VoteMessage> signedVotes = new ConcurrentHashMap<>();
}
