package com.limechain.network.protocol.grandpa.messages.catchup.res;

import com.limechain.exception.WrongMessageTypeException;
import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt32Reader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;

public class CatchUpMessageScaleReader implements ScaleReader<CatchUpMessage> {

    private static final CatchUpMessageScaleReader INSTANCE = new CatchUpMessageScaleReader();

    private final UInt32Reader uint32Reader;
    private final UInt64Reader uint64Reader;
    private final ListReader<SignedVote> signedVoteListReader;

    private CatchUpMessageScaleReader() {
        uint32Reader = new UInt32Reader();
        uint64Reader = new UInt64Reader();
        signedVoteListReader = new ListReader<>(SignedVoteScaleReader.getInstance());
    }

    public static CatchUpMessageScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public CatchUpMessage read(ScaleCodecReader reader) {
        int messageType = reader.readByte();
        if (messageType != GrandpaMessageType.CATCH_UP_RESPONSE.getType()) {
            throw new WrongMessageTypeException(
                    String.format("Trying to read message of type %d as a catch up response message", messageType));
        }

        CatchUpMessage catchUpMessage = new CatchUpMessage();
        catchUpMessage.setSetId(uint64Reader.read(reader));
        catchUpMessage.setRound(uint64Reader.read(reader));
        catchUpMessage.setPreVotes(signedVoteListReader
                .read(reader).toArray(SignedVote[]::new));
        catchUpMessage.setPreCommits(signedVoteListReader
                .read(reader).toArray(SignedVote[]::new));
        catchUpMessage.setBlockHash(new Hash256(reader.readUint256()));
        catchUpMessage.setBlockNumber(BigInteger.valueOf(uint32Reader.read(reader)));

        return catchUpMessage;
    }
}
