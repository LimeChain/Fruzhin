package com.limechain.internal.trie;

import com.limechain.internal.Node;
import org.junit.jupiter.api.Test;

public class TrieProofLoaderTest {

    @Test
    public void leafNode() {
        Node node = new Node(){{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }};

        Node expectedNode = new Node(){{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }};
    }
}
