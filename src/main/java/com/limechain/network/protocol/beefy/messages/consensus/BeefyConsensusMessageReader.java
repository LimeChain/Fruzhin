package com.limechain.network.protocol.beefy.messages.consensus;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeefyConsensusMessageReader implements ScaleReader<BeefyConsensusMessage> {

    private static final BeefyConsensusMessageReader INSTANCE = new BeefyConsensusMessageReader();
    public static final int ECDSA_PUBLIC_KEY_LENGTH = 33;

    public static BeefyConsensusMessageReader getInstance() {
        return INSTANCE;
    }

    @Override
    public BeefyConsensusMessage read(ScaleCodecReader reader) {

        BeefyConsensusMessage beefyConsensusMessage = new BeefyConsensusMessage();
        BeefyConsensusMessageFormat format = BeefyConsensusMessageFormat.fromFormat(reader.readByte());
        beefyConsensusMessage.setFormat(format);

        switch (format) {
            case BEEFY_CHANGED_AUTHORITIES -> {
                List<byte[]> authorityPublicKeys = new ListReader<>(
                        rdr -> rdr.readByteArray(ECDSA_PUBLIC_KEY_LENGTH)).read(reader);
                beefyConsensusMessage.setAuthorityPublicKeys(authorityPublicKeys);
                beefyConsensusMessage.setAuthoritySetId(new UInt64Reader().read(reader));
            }
            case BEEFY_ON_DISABLED -> beefyConsensusMessage.setDisabledAuthority(new UInt64Reader().read(reader));
            case BEEFY_MMR_ROOT -> beefyConsensusMessage.setMmrRootHash(reader.readUint256());
        }

        return beefyConsensusMessage;
    }
}
