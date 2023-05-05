package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.ForkTree;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;

public class ForkTreeNodeReader<T> implements ScaleReader<ForkTree.ForkTreeNode<T>> {
    private final ScaleReader<T> dataReader;

    public ForkTreeNodeReader(ScaleReader<T> dataReader) {
        this.dataReader = dataReader;
    }

    @Override
    public ForkTree.ForkTreeNode<T> read(ScaleCodecReader reader) {
        var node = new ForkTree.ForkTreeNode<T>();
        node.setHash(new Hash256(reader.readUint256()));
        node.setNumber(BigInteger.valueOf(reader.readUint32()));

        node.setData(dataReader.read(reader));

        node.setChildren(reader
                .read(new ListReader<>(new ForkTreeNodeReader<>(dataReader)))
                .toArray(ForkTree.ForkTreeNode[]::new)
        );
        return node;
    }
}
