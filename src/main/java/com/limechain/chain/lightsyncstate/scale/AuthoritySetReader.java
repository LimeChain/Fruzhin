package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.AuthoritySet;
import com.limechain.chain.lightsyncstate.ForkTree;
import com.limechain.chain.lightsyncstate.PendingChange;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt32Reader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthoritySetReader implements ScaleReader<AuthoritySet> {

    private static final AuthoritySetReader INSTANCE = new AuthoritySetReader();

    public static AuthoritySetReader getInstance() {
        return INSTANCE;
    }

    @Override
    public AuthoritySet read(ScaleCodecReader reader) {
        AuthoritySet authoritySet = new AuthoritySet();

        var authorities = reader.read(new ListReader<>(AuthorityReader.getInstance()));
        var setId = new UInt64Reader().read(reader);

        authoritySet.setAuthoritySet(new GrandpaAuthoritySet(setId, authorities));

        var forkTree = new ForkTree<>();
        forkTree.setRoots(reader.read(new ListReader<>(
                new ForkTreeNodeReader<>(PendingChangeReader.getInstance()))
        ).toArray(ForkTree.ForkTreeNode[]::new));
        forkTree.setBestFinalizedNumber(reader.readOptional(new UInt32Reader()));

        authoritySet.setPendingForcedChanges(reader.read(
                new ListReader<>(PendingChangeReader.getInstance())).toArray(PendingChange[]::new)
        );

        authoritySet.setAuthoritySetChanges(
                reader.read(new ListReader<>(AuthoritySetChangeReader.getInstance())).toArray(Pair[]::new)
        );

        return authoritySet;
    }
}
