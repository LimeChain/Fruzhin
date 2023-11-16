package com.limechain.storage.block.tree;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Node represents element in the block tree
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockNode {
    private byte[] hash; // Block hash
    private BlockNode parent; // Parent node
    private List<BlockNode> children = new ArrayList<>(); // Nodes of children blocks
    private long number; // block number
    private Instant arrivalTime; // Time of arrival of the block
    private boolean isPrimary; // whether the block was authored in a primary slot or not

    public BlockNode(byte[] hash, BlockNode parent, long number) {
        this.hash = hash;
        this.parent = parent;
        this.number = number;
        arrivalTime = Instant.now();
    }

    /**
     * Adds a child to the node
     * @param blockNode child to be added node
     */
    public void addChild(final BlockNode blockNode) {
        this.children.add(blockNode);
    }

    /**
     * @return stringified hash and number of node
     */
    @Override
    public String toString() {
        return String.format("{hash: %s, number: %d, arrivalTime: %s}", new Hash256(hash), number, arrivalTime);
    }

    /**
     * Recursively searches for a node with a given hash
     * @param hash hash of the node
     * @return node with the given hash
     */
    public BlockNode getNode(final byte[] hash) {
        if (Arrays.equals(this.hash, hash)) {
            return this;
        }
        for (BlockNode child : this.children) {
            BlockNode blockNode = child.getNode(hash);
            if (blockNode != null) {
                return blockNode;
            }
        }
        return null;
    }

    /**
     * Recursively searches for all nodes with a given number
     * @param number number of the block
     * @return list of nodes with the given number
     */
    public List<byte[]> getNodesWithNumber(final long number) {
        List<byte[]> hashes = new ArrayList<>();
        for (BlockNode child : children) {
            if (child.number == number) {
                hashes.add(child.hash);
            }
            if (child.number > number) {
                return hashes;
            }
            hashes.addAll(child.getNodesWithNumber(number));
        }
        return hashes;
    }

    /**
     * Traverse the tree following the parent nodes to verify if the given node is a descendant of the parent
     * @param parent parent node to be traversed
     * @return true if the node is a descendant of the parent
     */
    public boolean isDescendantOf(final BlockNode parent) {
        if (parent == null) {
            return false;
        }

        if (Arrays.equals(this.hash, parent.hash)) {
            return true;
        }

        if (parent.children.isEmpty()) {
            return false;
        }

        for (BlockNode child : parent.children) {
            if (this.isDescendantOf(child)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Recursively searches for all leaves of the tree
     * @return list of leaves
     */
    public List<BlockNode> getLeaves() {
        List<BlockNode> leaves = new ArrayList<>();
        if (this.children.isEmpty()) {
            leaves.add(this);
        }
        for (BlockNode child : children) {
            leaves.addAll(child.getLeaves());
        }
        return leaves;
    }

    /**
     * Recursively searches for all descendants of the node
     * @return list of the hash of all descendants
     */
    public List<byte[]> getAllDescendants() {
        List<byte[]> desc = new ArrayList<>();
        desc.add(this.hash);
        for (BlockNode child : children) {
            desc.addAll(child.getAllDescendants());
        }
        return desc;
    }

    /**
     * Creates a deep copy of the node of this node
     * @param parent parent of the node
     * @return deep copy of the node
     */
    public BlockNode deepCopy(BlockNode parent) {
        BlockNode copy = new BlockNode(this.hash, parent, new ArrayList<>(), this.number, this.arrivalTime, this.isPrimary);
        for (BlockNode child : children) {
            copy.children.add(child.deepCopy(copy));
        }
        return copy;
    }

    /**
     * Prune the tree by removing all nodes that are not descendants of the finalized node
     * @param finalized finalized node to be pruned
     * @return list of hashes of the pruned nodes
     */
    public List<byte[]> prune(BlockNode finalized) {
        List<byte[]> pruned = new ArrayList<>();
        if (finalized == null) {
            return pruned;
        }

        // if this is a descedent of the finalized block, keep it
        // all descendents of this block will also be descendents of the finalized block,
        // so don't need to check any of those
        if (this.isDescendantOf(finalized)) {
            return pruned;
        }

        // if it's not an ancestor of the finalized block, prune it
        if (!finalized.isDescendantOf(this)) {
            pruned.add(this.hash);
            if (this.parent != null) {
                this.parent.deleteChild(this);
            }
        }

        // if this is an ancestor of the finalized block, keep it,
        // and check its children
        for (BlockNode child : new ArrayList<>(children)) {
            pruned.addAll(child.prune(finalized));
        }

        return pruned;
    }

    /**
     * Deletes a child from the node
     * @param toDelete child to be deleted
     */
    public void deleteChild(BlockNode toDelete) {
        children.removeIf(child -> Arrays.equals(child.hash, toDelete.hash));
    }

    /**
     * Counts the number of primary ancestors of the node
     * @return number of primary ancestors
     */
    public int primaryAncestorCount() {
        int count = 0;
        if (this.isPrimary && this.parent != null) {
            count++;
        }

        return count + (this.parent != null ? this.parent.primaryAncestorCount() : 0);
    }
}
