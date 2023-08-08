package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.dto.Precommit;
import lombok.Data;

import java.math.BigInteger;

@Data
public class CommitMessage {
    private BigInteger roundNumber;
    private BigInteger setId;
    private Vote vote;
    private Precommit[] precommits;
}
