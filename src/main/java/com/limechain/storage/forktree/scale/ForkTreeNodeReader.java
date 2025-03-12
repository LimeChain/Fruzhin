package com.limechain.storage.forktree.scale;

import com.limechain.storage.forktree.ForkTree;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;
import java.util.List;

public class ForkTreeNodeReader<T> implements ScaleReader<ForkTree.ForkTreeNode<T>> {
    private final ScaleReader<T> dataReader;

    public ForkTreeNodeReader(ScaleReader<T> dataReader) {
        this.dataReader = dataReader;
    }

    @Override
    public ForkTree.ForkTreeNode<T> read(ScaleCodecReader reader) {

        Hash256 hash = new Hash256(reader.readUint256());
        BigInteger number = BigInteger.valueOf(reader.readUint32());
        T data = dataReader.read(reader);
        List<ForkTree.ForkTreeNode<T>> children = reader
                .read(new ListReader<>(new ForkTreeNodeReader<>(dataReader)));

        return new ForkTree.ForkTreeNode(hash, number, data, children);
    }
}
