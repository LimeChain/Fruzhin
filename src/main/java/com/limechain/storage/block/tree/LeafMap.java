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

    private final Map<byte[], BlockNode> syncMap = new HashMap<>();

    /**
     * Creates a new LeafMap from a node.
     *
     * @param blockNode the node to be converted to a map
     */
    public LeafMap(final BlockNode blockNode) {
        for (final BlockNode leaf : blockNode.getLeaves()) {
            syncMap.put(leaf.getHash(), leaf);
        }
    }

    /**
     * Stores a leaf in the map.
     *
     * @param key   hash of the leaf
     * @param value the leaf
     */
    public void store(byte[] key, BlockNode value) {
        syncMap.put(key, value);
    }

    /**
     * Loads a leaf from the map.
     *
     * @param key hash of the leaf
     * @return the leaf
     */
    public BlockNode load(byte[] key) {
        return syncMap.get(key);
    }

    /**
     * Deletes the old node from the map and inserts the new one.
     *
     * @param oldBlockNode the leaf to be removed
     * @param newBlockNode the leaf to be added
     */
    public void replace(BlockNode oldBlockNode, BlockNode newBlockNode) {
        syncMap.remove(oldBlockNode.getHash());
        store(newBlockNode.getHash(), newBlockNode);
    }

    /**
     * Searches the stored leaves to the find the one with the greatest number.
     * If there are two leaves with the same number, choose the one with the earliest arrival time.
     *
     * @return the leaf with the greatest number
     */
    public BlockNode highestLeaf() {
        BlockNode deepest = null;
        long max = 0;
        for (BlockNode blockNode : syncMap.values()) {
            if (blockNode.getNumber() > max) {
                max = blockNode.getNumber();
                deepest = blockNode;
            } else if (blockNode.getNumber() == max && deepest != null && (
                    (blockNode.getArrivalTime().isBefore(deepest.getArrivalTime())) ||
                            // there are two leaf nodes with the same number *and* arrival time, just pick the one
                            // with the lower hash in lexicographical order.
                            // practically, this is very unlikely to happen.
                            (blockNode.getArrivalTime().equals(deepest.getArrivalTime()) &&
                                    (Arrays.compare(blockNode.getHash(), deepest.getHash()) < 0))
            )) {
                deepest = blockNode;

            }
        }
        return deepest;
    }

    /**
     * Get list of all nodes in the map.
     *
     * @return list of all nodes
     */
    public List<BlockNode> nodes() {
        return new ArrayList<>(syncMap.values());
    }

    /**
     * Find the best non finalized block.
     *
     * @return the best block
     */
    public BlockNode bestBlock() {
        Map<Integer, List<BlockNode>> counts = new HashMap<>();
        int highest = 0;

        //Find the block with the highest ancestors count
        for (BlockNode blockNode : syncMap.values()) {
            int count = blockNode.primaryAncestorCount();
            if (count > highest) {
                highest = count;
            }

            counts.computeIfAbsent(count, initialCapacity ->  /*explicitly use initialCapacity*/
                    new ArrayList<>(initialCapacity)).add(blockNode);
        }

        //If there is only one block with the highest count, return it
        if (counts.get(highest).size() == 1) {
            return counts.get(highest).get(0);
        }

        //If there are multiple blocks with the highest count, return the one with the highest leaf count
        LeafMap lm2 = new LeafMap();
        for (BlockNode blockNode : counts.get(highest)) {
            lm2.store(blockNode.getHash(), blockNode);
        }

        return lm2.highestLeaf();
    }

}
