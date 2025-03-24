package com.limechain.consensus.grandpa.scale;

import com.limechain.chain.lightsyncstate.scale.AuthorityReader;
import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GrandpaAuthoritySetReader implements ScaleReader<GrandpaAuthoritySet> {

    private static final GrandpaAuthoritySetReader INSTANCE = new GrandpaAuthoritySetReader();

    public static GrandpaAuthoritySetReader getInstance() {
        return INSTANCE;
    }

    @Override
    public GrandpaAuthoritySet read(ScaleCodecReader reader) {

        List<Authority> authorities = reader.read(new ListReader<>(AuthorityReader.getInstance()));
        BigInteger setId = new UInt64Reader().read(reader);

        return new GrandpaAuthoritySet(setId, authorities);

        // The following code is retained (but commented out because not used at the moment) to
        // demonstrate how the pending scheduled/forced changes data, best finalized number and
        // authority set changes can be read if needed.

//        ForkTree<PendingChange> forkTree = new ForkTree<>();
//        forkTree.setRoots(reader.read(new ListReader<>(
//                new ForkTreeNodeReader<>(PendingChangeReader.getInstance()))
//        ));
//
//        Optional<Long> bestFinalizedNumber = reader.readOptional(new UInt32Reader());
//        forkTree.setBestFinalizedNumber(
//                bestFinalizedNumber.map(BigInteger::valueOf).orElse(null)
//        );
//
//        authoritySet.setPendingForcedChanges(reader.read(
//                new ListReader<>(PendingChangeReader.getInstance()))
//        );
//
//        authoritySet.setAuthoritySetChanges(
//                reader.read(new ListReader<>(AuthoritySetChangeReader.getInstance()))
//        );
    }
}
