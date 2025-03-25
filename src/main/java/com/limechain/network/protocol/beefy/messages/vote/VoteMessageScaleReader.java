package com.limechain.network.protocol.beefy.messages.vote;

import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.scale.CommitmentScaleReader;
import com.limechain.exception.scale.WrongMessageTypeException;
import com.limechain.network.protocol.beefy.messages.BeefyMessageType;
import com.limechain.utils.EcdsaUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VoteMessageScaleReader implements ScaleReader<VoteMessage> {

    private static final VoteMessageScaleReader INSTANCE = new VoteMessageScaleReader();

    public static VoteMessageScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public VoteMessage read(ScaleCodecReader reader) {

        int messageType = reader.readByte();
        if (messageType != BeefyMessageType.VOTE.getType()) {
            throw new WrongMessageTypeException(
                    String.format("Trying to read message of type %d as a beefy vote message", messageType));
        }

        Commitment commitment = reader.read(CommitmentScaleReader.getInstance());
        byte[] authorityId = reader.readByteArray(EcdsaUtils.PUBLIC_KEY_COMPRESSED_LEN);
        byte[] signature = reader.readByteArray(EcdsaUtils.SIGNATURE_LEN);

        return new VoteMessage(commitment, authorityId, signature);
    }
}
