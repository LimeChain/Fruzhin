package com.limechain.trie.structure;

import com.limechain.trie.structure.nibble.Nibble;

import java.util.stream.Collectors;

/**
 * Utility class for visually debugging the TrieStructure
 * Serializes it to a dot script for easy rendering using tools like GraphViz
 * @param <T> the UserData type parameter of the TrieStructure
 */
class TrieStructureDotSerializer<T> {
    TrieStructure<T> trie;

    TrieStructureDotSerializer(TrieStructure<T> trie) {
        this.trie = trie;
    }

    public static <T> String serialize(TrieStructure<T> trie) {
        return new TrieStructureDotSerializer<>(trie).serialize();
    }

    private static final String TEMPLATE = """
            digraph trie {
                splines=compound;
                rankdir=TB;
                node [shape=record];
                        
            %s
            }""";

    String serialize() {
        StringBuilder body = new StringBuilder();
        trie.nodes.getAllEntries().forEach(p -> {
            var node = p.getValue1();
            var nodeIndex = p.getValue0();
            if (node.parent != null) {
                int parentIndex = node.parent.parentNodeIndex();
                Nibble childIndexWithinParent = node.parent.childIndexWithinParent();
                body.append(String.format(
                    "    %d:%c -> %d;",
                    parentIndex, childIndexWithinParent.asLowerHexDigit(), nodeIndex));
                body.append(System.lineSeparator());
            }
            body.append(serializeNode(node, nodeIndex));
            body.append(System.lineSeparator());
            body.append(System.lineSeparator());
        });

        return String.format(TEMPLATE, body);
    }

    private static final String CHILDREN_INDICES;
    static {
        String childrenIndices = Nibble.all().map(childIndex -> String.format("<%1$s>%1$s", childIndex)).collect(
            Collectors.joining(" | "));
        CHILDREN_INDICES = String.format("{%s}", childrenIndices);
    }

    String serializeNode(TrieNode<T> node, int nodeIndex) {
        String partialKey = String.format("partialKey: %s", node.partialKey);
        String hasStorageValue = String.format("hasValue?: %b", node.hasStorageValue);
        String userData = String.format("userData: %s", node.userData);
        String idx = String.format("Node index: %d", nodeIndex);

        String label = String.join(" | ", partialKey, hasStorageValue, userData, idx, CHILDREN_INDICES);

        return String.format("    %d[label=\"{%s}\"];", nodeIndex, label);
    }
}
