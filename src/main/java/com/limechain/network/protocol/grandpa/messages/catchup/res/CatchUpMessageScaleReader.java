package com.limechain.network.protocol.grandpa.messages.catchup.res;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt32Reader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;

public class CatchUpMessageScaleReader implements ScaleReader<CatchUpMessage> {
    @Override
    public CatchUpMessage read(ScaleCodecReader reader) {
        int messageType = reader.readByte();
        if (messageType != GrandpaMessageType.CATCH_UP_RESPONSE.getType()) {
            throw new RuntimeException(
                    String.format("Trying to read message of type %d as a catch up response message", messageType));
        }

        CatchUpMessage catchUpMessage = new CatchUpMessage();
        catchUpMessage.setSetId(new UInt64Reader().read(reader));
        catchUpMessage.setRound(new UInt64Reader().read(reader));
        catchUpMessage.setPreVotes(new ListReader<>(new SignedVoteScaleReader())
                .read(reader).toArray(SignedVote[]::new));
        catchUpMessage.setPreCommits(new ListReader<>(new SignedVoteScaleReader())
                .read(reader).toArray(SignedVote[]::new));
        catchUpMessage.setBlockHash(new Hash256(reader.readUint256()));
        catchUpMessage.setBlockNumber(BigInteger.valueOf(new UInt32Reader().read(reader)));

        return catchUpMessage;
    }
}
