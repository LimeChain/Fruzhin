package com.limechain.network.protocol.grandpa.messages.vote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteMessage {
    private BigInteger roundNumber;
    private BigInteger setId;
    private SignedMessage message;
}
