package com.limechain.consensus.beefy.scale;

import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.limechain.consensus.beefy.scale.message.BeefyConsensusMessageReader.ECDSA_PUBLIC_KEY_LENGTH;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeefyAuthoritySetReader implements ScaleReader<BeefyAuthoritySet> {

    private static final BeefyAuthoritySetReader INSTANCE = new BeefyAuthoritySetReader();

    public static BeefyAuthoritySetReader getInstance() {
        return INSTANCE;
    }

    @Override
    public BeefyAuthoritySet read(ScaleCodecReader reader) {
        return new BeefyAuthoritySet(new ListReader<>(
                rdr -> rdr.readByteArray(ECDSA_PUBLIC_KEY_LENGTH)).read(reader), new UInt64Reader().read(reader));
    }
}
