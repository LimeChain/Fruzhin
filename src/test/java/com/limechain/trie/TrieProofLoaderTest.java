package com.limechain.trie;

import com.limechain.trie.decoder.TrieDecoderException;
import com.limechain.trie.encoder.TrieEncoder;
import com.limechain.utils.HashUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrieProofLoaderTest {

    @Test
    void loadLeafNodeTest() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }};

        Node expectedNode = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }};
        TrieProofLoader.loadProof(null, node);
        assertEquals(expectedNode.toString(), node.toString());
    }

    @Test
    void loadBranchChildWithNoHashTest() {
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

        TrieProofLoader.loadProof(digestToEncoding, node);
        assertEquals(expectedNode.toString(), node.toString());
    }

    @Test
    void loadBranchNodeWithHashTest() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setMerkleValue(new byte[]{2});
                    }}
            }));
        }};
        ByteArrayOutputStream encodedNode = new ByteArrayOutputStream();
        Node nodeToEncode = new Node() {{
            this.setPartialKey(new byte[]{3});
            this.setStorageValue(new byte[]{1});
        }};
        TrieEncoder.encode(nodeToEncode, encodedNode);
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(HexUtils.toHexString(new byte[]{2}), encodedNode.toByteArray());
        }};

        Node expectedNode = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(new byte[]{3});
                        this.setStorageValue(new byte[]{1});
                        this.setDirty(true);
                    }}
            }));
        }};

        TrieProofLoader.loadProof(digestToEncoding, node);
        assertEquals(expectedNode.toString(), node.toString());
    }

    @Test
    void loadBranchOneChildWithHashAndOneWithoutHashTest() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(2);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setMerkleValue(new byte[]{2});
                    }},
                    new Node() {{
                        this.setMerkleValue(new byte[]{3});
                    }}
            }));
        }};
        ByteArrayOutputStream encodedNode = new ByteArrayOutputStream();
        Node nodeToEncode = new Node() {{
            this.setPartialKey(new byte[]{3});
            this.setStorageValue(new byte[]{1});
        }};
        TrieEncoder.encode(nodeToEncode, encodedNode);
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(HexUtils.toHexString(new byte[]{2}), encodedNode.toByteArray());
        }};

        Node expectedNode = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(new byte[]{3});
                        this.setStorageValue(new byte[]{1});
                        this.setDirty(true);
                    }}
            }));
        }};

        TrieProofLoader.loadProof(digestToEncoding, node);
        assertEquals(expectedNode.toString(), node.toString());
    }

    @Test
    void loadBranchNodeWithBranchChildHash() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(2);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setMerkleValue(new byte[]{2});
                    }}
            }));
        }};
        ByteArrayOutputStream encodedNode = new ByteArrayOutputStream();
        Node nodeToEncode = new Node() {{
            this.setPartialKey(new byte[]{3});
            this.setStorageValue(new byte[]{1});
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(new byte[]{4});
                        this.setStorageValue(new byte[]{2});
                    }}}
            ));
        }};
        TrieEncoder.encode(nodeToEncode, encodedNode);
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(HexUtils.toHexString(new byte[]{2}), encodedNode.toByteArray());
        }};

        Node expectedNode = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(3);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(new byte[]{3});
                        this.setStorageValue(new byte[]{1});
                        this.setDirty(true);
                        this.setDescendants(1);
                        this.setChildren(Helper.padRightChildren(new Node[]{
                                new Node() {{
                                    this.setPartialKey(new byte[]{4});
                                    this.setStorageValue(new byte[]{2});
                                    this.setDirty(true);
                                }}
                        }));
                    }}
            }));
        }};

        TrieProofLoader.loadProof(digestToEncoding, node);
        assertEquals(expectedNode.toString(), node.toString());
    }

    @Test
    void loadChildExceptionTest() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setMerkleValue(new byte[]{2});
                    }}
            }));
        }};
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(HexUtils.toHexString(new byte[]{2}), Helper.getBadNodeEncoding());
        }};


        Exception e = assertThrows(TrieDecoderException.class, () -> TrieProofLoader.loadProof(digestToEncoding, node));
        assertTrue(e.getMessage().contains("Unknown variant: COMPACT_ENCODING"));
    }

    @Test
    void loadGrandChild() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{1});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setMerkleValue(new byte[]{2});
                    }}
            }));
        }};
        ByteArrayOutputStream encodedNode = new ByteArrayOutputStream();
        ByteArrayOutputStream encodedLeaf = new ByteArrayOutputStream();
        Node nodeToEncode = new Node() {{
            this.setPartialKey(new byte[]{2});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    Helper.leafBLarge
            }));
        }};
        TrieEncoder.encode(nodeToEncode, encodedNode);
        TrieEncoder.encode(Helper.leafBLarge, encodedLeaf);
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(HexUtils.toHexString(new byte[]{2}), encodedNode.toByteArray());
            put(HexUtils.toHexString(Helper.leafBLarge.calculateMerkleValue()), encodedLeaf.toByteArray());
        }};

        Node expectedNode = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{1});
            this.setDescendants(2);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(new byte[]{2});
                        this.setStorageValue(new byte[]{2});
                        this.setDescendants(1);
                        this.setDirty(true);
                        this.setChildren(Helper.padRightChildren(new Node[]{
                                new Node() {{
                                    this.setPartialKey(Helper.leafBLarge.getPartialKey());
                                    this.setStorageValue(Helper.leafBLarge.getStorageValue());
                                    this.setDirty(true);
                                }}
                        }));
                    }}
            }));
        }};

        TrieProofLoader.loadProof(digestToEncoding, node);
        assertEquals(expectedNode.toString(), node.toString());
    }

    @Test
    void loadGrandChildExceptionTest() {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{1});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setMerkleValue(new byte[]{2});
                    }}
            }));
        }};
        ByteArrayOutputStream encodedNode = new ByteArrayOutputStream();
        ByteArrayOutputStream encodedLeaf = new ByteArrayOutputStream();
        Node nodeToEncode = new Node() {{
            this.setPartialKey(new byte[]{2});
            this.setStorageValue(new byte[]{2});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    Helper.leafBLarge
            }));
        }};
        TrieEncoder.encode(nodeToEncode, encodedNode);
        TrieEncoder.encode(Helper.leafBLarge, encodedLeaf);
        String encodedLeafKey = HexUtils.toHexString(HashUtils.hashWithBlake2b(encodedLeaf.toByteArray()));
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(HexUtils.toHexString(new byte[]{2}), encodedNode.toByteArray());
            put(encodedLeafKey, Helper.getBadNodeEncoding());
        }};

        Exception e = assertThrows(TrieDecoderException.class, () -> TrieProofLoader.loadProof(digestToEncoding, node));
        assertTrue(e.getMessage().contains("Unknown variant: COMPACT_ENCODING"));
    }
}
