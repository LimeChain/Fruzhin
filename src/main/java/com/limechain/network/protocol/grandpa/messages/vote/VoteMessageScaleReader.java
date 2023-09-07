package com.limechain.network.protocol.grandpa.messages.vote;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;

public class VoteMessageScaleReader implements ScaleReader<VoteMessage> {
    @Override
    public VoteMessage read(ScaleCodecReader reader) {
        int messageType = reader.readByte();
        if (messageType != GrandpaMessageType.VOTE.getType()) {
            throw new RuntimeException(
                    String.format("Trying to read message of type %d as a vote message", messageType));
        }

        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setRoundNumber(new UInt64Reader().read(reader));
        voteMessage.setSetId(new UInt64Reader().read(reader));
        voteMessage.setMessage(new SignedMessageScaleReader().read(reader));

        return voteMessage;
    }
}
