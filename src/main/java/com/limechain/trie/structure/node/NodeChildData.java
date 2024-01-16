package com.limechain.trie.structure.node;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * Represents the data associated with a child node in a trie structure.
 * This class includes the child node's number and its hash.
 */
@AllArgsConstructor
@EqualsAndHashCode
public class NodeChildData {
    /** The number assigned to the child node, representing its order. */
    int childNum;

    /** The hash of the child node, uniquely identifying it within the trie. */
    byte[] childHash;
}
