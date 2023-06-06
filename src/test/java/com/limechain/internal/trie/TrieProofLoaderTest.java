package com.limechain.internal.trie;

import com.limechain.internal.Node;
import com.limechain.internal.TreeEncoder;
import com.limechain.internal.tree.decoder.TrieDecoderException;
import com.limechain.utils.HashUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.limechain.internal.trie.Helper.leafBLarge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrieProofLoaderTest {

    @Test
    public void loadLeafNodeTest() throws TrieDecoderException {
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
    public void loadBranchChildWithNoHashTest() throws TrieDecoderException {
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
    public void loadBranchNodeWithHashTest() throws Exception {
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
        TreeEncoder.encode(nodeToEncode, encodedNode);
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(new String(new byte[]{2}), encodedNode.toByteArray());
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
    public void loadBranchOneChildWithHashAndOneWithoutHashTest() throws Exception {
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
        TreeEncoder.encode(nodeToEncode, encodedNode);
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(new String(new byte[]{2}), encodedNode.toByteArray());
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
    public void loadBranchNodeWithBranchChildHash() throws Exception {
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
        TreeEncoder.encode(nodeToEncode, encodedNode);
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(new String(new byte[]{2}), encodedNode.toByteArray());
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
                        this.setChildren(new Node[]{
                                new Node() {{
                                    this.setPartialKey(new byte[]{4});
                                    this.setStorageValue(new byte[]{2});
                                    this.setDirty(true);
                                }}

                        });
                    }}
            }));
        }};

        TrieProofLoader.loadProof(digestToEncoding, node);
        assertEquals(expectedNode.toString(), node.toString());
    }

    @Test
    public void loadChildExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
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
                put(new String(new byte[]{2}), Helper.getBadNodeEncoding());
            }};

            Node expectedNode = new Node() {{
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
            // Catch exception here
            TrieProofLoader.loadProof(digestToEncoding, node);
        });
        assertTrue(e.getMessage().contains("Node variant is unknown for header byte 00000001"));
    }

    @Test
    public void loadGrandChild() throws Exception {
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
                    leafBLarge
            }));
        }};
        TreeEncoder.encode(nodeToEncode, encodedNode);
        TreeEncoder.encode(leafBLarge, encodedLeaf);
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(new String(new byte[]{2}), encodedNode.toByteArray());
            put(HashUtils.hashWithBlake2b(encodedLeaf.toByteArray()).toString(), encodedLeaf.toByteArray());
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
                                    this.setPartialKey(leafBLarge.getPartialKey());
                                    this.setStorageValue(leafBLarge.getStorageValue());
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
    public void loadGrandChildExceptionTest() throws Exception {
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
                    leafBLarge
            }));
        }};
        TreeEncoder.encode(nodeToEncode, encodedNode);
        TreeEncoder.encode(leafBLarge, encodedLeaf);
        String encodedLeafKey =  new String(HashUtils.hashWithBlake2b(encodedLeaf.toByteArray()));
        Map<String, byte[]> digestToEncoding = new HashMap<>() {{
            put(new String(new byte[]{2}), encodedNode.toByteArray());
            put(encodedLeafKey, Helper.getBadNodeEncoding());
        }};
        Node expectedNode = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{1});
            this.setDescendants(1);
            this.setDirty(true);
            this.setChildren(Helper.padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(new byte[]{2});
                        this.setStorageValue(new byte[]{2});
                        this.setDescendants(1);
                        this.setDirty(true);
                        this.setChildren(Helper.padRightChildren(new Node[]{
                                new Node() {{
                                    this.setMerkleValue(HashUtils.hashWithBlake2b(encodedLeaf.toByteArray()));
                                }}
                        }));
                    }}
            }));
        }};

        TrieProofLoader.loadProof(digestToEncoding, node);
        assertEquals(expectedNode.toString(), node.toString());
    }
}
