package com.limechain.storage.block.tree;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A map of leaves that provides quick lookup for existing leaves.
 */
@Getter
@NoArgsConstructor
public class LeafMap {

    private final Map<byte[], Node> syncMap = new HashMap<>();

    /**
     * Creates a new LeafMap from a node.
     * @param node the node to be converted to a map
     */
    public LeafMap(final Node node){
        for (final Node leaf : node.getLeaves()) {
            syncMap.put(leaf.getHash(), leaf);
        }
    }

    /**
     * Stores a leaf in the map.
     * @param key hash of the leaf
     * @param value the leaf
     */
    public void store(byte[] key, Node value) {
        syncMap.put(key, value);
    }

    /**
     * Loads a leaf from the map.
     * @param key hash of the leaf
     * @return the leaf
     * @throws RuntimeException if the leaf is not found
     */
    public Node load(byte[] key) {
        return syncMap.get(key);
    }

    /**
     * Deletes the old node from the map and inserts the new one.
     * @param oldNode the leaf to be removed
     * @param newNode the leaf to be added
     */
    public void replace(Node oldNode, Node newNode) {
        syncMap.remove(oldNode.getHash());
        store(newNode.getHash(), newNode);
    }

    /**
     * Searches the stored leaves to the find the one with the greatest number.
     * If there are two leaves with the same number, choose the one with the earliest arrival time.
     * @return the leaf with the greatest number
     */
    public Node highestLeaf() {
        Node deepest = null;
        long max = 0;
        for (Node node : syncMap.values()) {
            if (node.getNumber() > max) {
                max = node.getNumber();
                deepest = node;
            } else if (node.getNumber() == max && deepest != null) {
                if (node.getArrivalTime().isBefore(deepest.getArrivalTime())) {
                    deepest = node;
                }else if (node.getArrivalTime().equals(deepest.getArrivalTime())){
                    // there are two leaf nodes with the same number *and* arrival time, just pick the one
                    // with the lower hash in lexicographical order.
                    // practically, this is very unlikely to happen.
                    if (Arrays.compare(node.getHash(), deepest.getHash()) < 0){
                        deepest = node;
                    }
                }
            }
        }
        return deepest;
    }

    /**
     * Get list of all nodes in the map.
     * @return list of all nodes
     */
    public List<Node> nodes() {
        return new ArrayList<>(syncMap.values());
    }

    /**
     * Find the best block.
     * @return the best block
     */
    public Node bestBlock() {
        Map<Integer, List<Node>> counts = new HashMap<>();
        int highest = 0;

        //Find the block with the highest ancestors count
        for (Node node : syncMap.values()) {
            int count = node.primaryAncestorCount();
            if (count > highest) {
                highest = count;
            }

            counts.computeIfAbsent(count, ArrayList::new).add(node);
        }

        //If there is only one block with the highest count, return it
        if (counts.get(highest).size() == 1) {
            return counts.get(highest).get(0);
        }

        //If there are multiple blocks with the highest count, return the one with the highest leaf count
        LeafMap lm2 = new LeafMap();
        for (Node node : counts.get(highest)) {
            lm2.store(node.getHash(), node);
        }

        return lm2.highestLeaf();
    }

}
