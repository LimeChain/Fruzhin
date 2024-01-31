package com.limechain.trie.decoded;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TrieTest {

    @Test
    void getTest() {
        byte[] key = new byte[]{0x01, 0x19};
        byte[] expectedValue = new byte[]{1, 2, 3, 4, 5};

        Node node = new Node() {{
            this.setPartialKey(new byte[]{0, 1});
            this.setStorageValue(new byte[]{1, 3});
            this.setDescendants(3);
            this.setChildren(
                    Helper.padRightChildren(new Node[]{
                            new Node() {{
                                this.setPartialKey(new byte[]{3});
                                this.setStorageValue(new byte[]{1, 2});
                                this.setDescendants(1);
                                this.setChildren(Helper.padRightChildren(new Node[]{
                                        new Node() {{
                                            this.setPartialKey(new byte[]{1});
                                            this.setStorageValue(new byte[]{1});
                                        }},
                                }));

                            }},
                            new Node() {{
                                this.setPartialKey(new byte[]{9});
                                this.setStorageValue(new byte[]{1, 2, 3, 4, 5});
                            }}
                    })
            );
        }};
        Trie trie = Trie.newTrie(node);
        byte[] value = trie.get(key);
        assertArrayEquals(expectedValue, value);
    }

    @Test
    void retrieveNullParentTest() {
        byte[] key = new byte[]{1};
        Trie trie = Trie.newTrie(null);
        assertArrayEquals(new byte[0], trie.get(key));
    }

    @Test
    void retrieveMatchLeafKeyTest() {
        byte[] key = new byte[]{1};
        byte[] expectedValue = new byte[]{2};
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }};
        byte[] value = Trie.retrieve(node, key);
        assertArrayEquals(expectedValue, value);
    }

    @Test
    void leafKeyMismatchTest() {
        byte[] key = new byte[]{1};
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1, 2});
            this.setStorageValue(new byte[]{2});
        }};
        assertArrayEquals(new byte[0], Trie.retrieve(node, key));
    }

    @Test
    void branchKeyMatchTest() {
        byte[] key = new byte[]{1};
        byte[] expectedValue = new byte[]{2};
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(new byte[]{1});
                        this.setStorageValue(new byte[]{1});
                    }}
            }));
        }};
        byte[] value = Trie.retrieve(node, key);
        assertArrayEquals(expectedValue, value);
    }

    @Test
    void branchKeyWithEmptySearchKeyTest() {
        byte[] key = new byte[]{};
        byte[] expectedValue = new byte[]{2};
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(new byte[]{1});
                        this.setStorageValue(new byte[]{1});
                    }}
            }));
        }};
        byte[] value = Trie.retrieve(node, key);
        assertArrayEquals(expectedValue, value);
    }

    @Test
    void matchBottomLeafInBranch() {
        byte[] key = new byte[]{1, 2, 3, 4, 5};
        byte[] expectedValue = new byte[]{3};

        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{1});
            this.setDescendants(2);
            this.setChildren(
                    Helper.padRightChildren(new Node[]{
                            null,
                            null,
                            new Node() {{
                                this.setPartialKey(new byte[]{3});
                                this.setStorageValue(new byte[]{2});
                                this.setDescendants(1);
                                this.setChildren(Helper.padRightChildren(new Node[]{
                                        null, null, null, null,
                                        new Node() {{
                                            this.setPartialKey(new byte[]{5});
                                            this.setStorageValue(new byte[]{3});
                                        }},
                                }));

                            }},
                            new Node() {{
                                this.setPartialKey(new byte[]{9});
                                this.setStorageValue(new byte[]{1, 2, 3, 4, 5});
                            }}
                    })
            );
        }};

        byte[] value = Trie.retrieve(node, key);
        assertArrayEquals(expectedValue, value);
    }
}
