package com.limechain.chain.lightsyncstate;

import com.limechain.polkaj.Hash256;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Optional;

@Getter
@Setter
public class ForkTree<T> {
    private ForkTreeNode<T>[] roots;
    private Optional<Long> bestFinalizedNumber;

    @Getter
    @Setter
    public static class ForkTreeNode<T> {
        private Hash256 hash;
        private BigInteger number;
        private T data;
        private ForkTreeNode<T>[] children;
    }
}
