package com.limechain.grandpa.state.scale;

import com.limechain.chain.lightsyncstate.scale.AuthorityReader;
import com.limechain.chain.lightsyncstate.scale.PendingChangeReader;
import com.limechain.storage.forktree.scale.ForkTreeNodeReader;
import com.limechain.grandpa.state.AuthoritySet;
import com.limechain.storage.forktree.ForkTree;
import com.limechain.chain.lightsyncstate.PendingChange;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt32Reader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthoritySetReader implements ScaleReader<AuthoritySet> {

    private static final AuthoritySetReader INSTANCE = new AuthoritySetReader();

    public static AuthoritySetReader getInstance() {
        return INSTANCE;
    }

    @Override
    public AuthoritySet read(ScaleCodecReader reader) {
        AuthoritySet authoritySet = new AuthoritySet();

        authoritySet.setAuthorities(
                reader.read(new ListReader<>(AuthorityReader.getInstance()))
        );

        authoritySet.setSetId(new UInt64Reader().read(reader));

        ForkTree<PendingChange> forkTree = new ForkTree<>();
        forkTree.setRoots(reader.read(new ListReader<>(
                new ForkTreeNodeReader<>(PendingChangeReader.getInstance()))
        ));

        Optional<Long> bestFinalizedNumber = reader.readOptional(new UInt32Reader());
        forkTree.setBestFinalizedNumber(
                bestFinalizedNumber.map(BigInteger::valueOf).orElse(null)
        );

        authoritySet.setPendingForcedChanges(reader.read(
                new ListReader<>(PendingChangeReader.getInstance()))
        );

        authoritySet.setAuthoritySetChanges(
                reader.read(new ListReader<>(AuthoritySetChangeReader.getInstance()))
        );

        return authoritySet;
    }
}
