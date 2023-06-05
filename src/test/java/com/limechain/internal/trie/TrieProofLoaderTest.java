package com.limechain.internal.trie;

import com.limechain.internal.Node;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class TrieProofLoaderTest {

    @Test
    public void loadLeafNodeTest() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }};

        Node expectedNode = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }};
    }

    @Test
    public void loadBranchChildWithNoHashTest() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setMerkleValue(new byte[]{3});
                    }}
            }));
        }};

        Map<String, byte[]> digestToEncoding = new HashMap<>();

        Node expectedNode = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDirty(true);
        }};
    }


}
