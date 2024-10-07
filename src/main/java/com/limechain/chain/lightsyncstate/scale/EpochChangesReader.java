package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.EpochChanges;
import com.limechain.chain.lightsyncstate.ForkTree;
import com.limechain.chain.lightsyncstate.PersistedEpoch;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt32Reader;
import io.emeraldpay.polkaj.types.Hash256;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

public class EpochChangesReader implements ScaleReader<EpochChanges> {
    @Override
    public EpochChanges read(ScaleCodecReader reader) {
        EpochChanges changes = new EpochChanges();

        var forkTree = new ForkTree<>();
        forkTree.setRoots(reader.read(new ListReader<>(
                        new ForkTreeNodeReader<>(
                                new PersistedEpochHeaderReader()
                        )))
                .toArray(ForkTree.ForkTreeNode[]::new));
        forkTree.setBestFinalizedNumber(reader.readOptional(new UInt32Reader()));

        Map<Pair<Hash256, BigInteger>, PersistedEpoch> epochs = new TreeMap<>();
        int epochsCount = reader.readCompactInt();
        for (int i = 0; i < epochsCount; i++) {
            Pair<Hash256, BigInteger> key = new Pair<>(
                    new Hash256(reader.readUint256()),
                    BigInteger.valueOf(reader.readUint32())
            );
            var value = reader.read(new PersistedEpochReader());
            epochs.put(key, value);
        }

        changes.setEpochs(epochs);
        return changes;
    }
}
