package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.EpochChanges;
import com.limechain.chain.lightsyncstate.ForkTree;
import com.limechain.chain.lightsyncstate.PersistedEpoch;
import com.limechain.chain.lightsyncstate.PersistedEpochHeader;
import com.limechain.polkaj.Hash256;
import com.limechain.polkaj.reader.ListReader;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;
import com.limechain.polkaj.reader.UInt32Reader;
import com.limechain.tuple.Pair;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpochChangesReader implements ScaleReader<EpochChanges> {
    @Override
    public EpochChanges read(ScaleCodecReader reader) {
        EpochChanges changes = new EpochChanges();

        var forkTree = new ForkTree<>();
        PersistedEpochHeaderReader epochHeaderReader = new PersistedEpochHeaderReader();
        ForkTreeNodeReader<PersistedEpochHeader> forkTreeReader = new ForkTreeNodeReader<>(epochHeaderReader);
        ListReader<ForkTree.ForkTreeNode<PersistedEpochHeader>> listReader = new ListReader<>(forkTreeReader);
        List<ForkTree.ForkTreeNode<PersistedEpochHeader>> roots = reader.read(listReader);
        forkTree.setRoots(roots.toArray(ForkTree.ForkTreeNode[]::new));
        forkTree.setBestFinalizedNumber(reader.readOptional(new UInt32Reader()));

        PersistedEpochReader scaleReader = new PersistedEpochReader();

        Map<Pair<Hash256, BigInteger>, PersistedEpoch> epochs = new HashMap<>();
        int epochsCount = reader.readCompactInt();
        for (int i = 0; i < epochsCount; i++) {
            Hash256 hash256 = new Hash256(reader.readUint256());
            BigInteger bigInteger = BigInteger.valueOf(reader.readUint32());
            Pair<Hash256, BigInteger> key = new Pair<>(hash256, bigInteger);
            var value = reader.read(scaleReader);
            epochs.put(key, value);
        }

        changes.setEpochs(epochs);
        return changes;
    }
}
