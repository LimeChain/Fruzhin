package com.limechain.network.protocol.beefy;

import com.limechain.consensus.beefy.BeefyService;
import com.limechain.consensus.beefy.scale.CommitmentScaleWriter;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import com.limechain.utils.EcdsaUtils;
import com.limechain.utils.HashUtils;
import com.limechain.utils.scale.ScaleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

@Log
@RequiredArgsConstructor
@Component
public class BeefyMessageHandler {

    private final BeefyService beefyService;

    public void handleVoteMessage(BeefyVoteMessage beefyVoteMessage) {

        if (!isVoteMessageValid(beefyVoteMessage)) {
            log.warning(String.format(
                    "handleBeefyVoteMessage: Invalid vote message for round %s, set %s",
                    beefyVoteMessage.getCommitment().getBlockNumber(), beefyVoteMessage.getCommitment().getAuthoritySetId()
            ));
            return;
        }

        beefyService.triageIncomingVote(beefyVoteMessage);
    }

    private boolean isVoteMessageValid(BeefyVoteMessage beefyVoteMessage) {

        byte[] encodedCommitment = HashUtils.hashWithKeccak256(ScaleUtils.Encode.encode(CommitmentScaleWriter.getInstance(),
                beefyVoteMessage.getCommitment()));

        VerifySignature verifySignature = new VerifySignature(
                beefyVoteMessage.getSignature(),
                encodedCommitment,
                beefyVoteMessage.getAuthorityId(),
                Key.ECDSA);

        return EcdsaUtils.verifySignature(verifySignature);
    }
}
