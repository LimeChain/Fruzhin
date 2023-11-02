package com.limechain.storage.block;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Node represents a element in the block tree
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private Hash256 hash; // Block hash
    private Node parent; // Parent node
    private List<Node> children = new ArrayList<>(); // Nodes of children blocks
    private long number; // block number
    private Instant arrivalTime; // Time of arrival of the block
    private boolean isPrimary; // whether the block was authored in a primary slot or not

    /**
     * Adds a child to the node
     * @param node child to be added node
     */
    public void addChild(final Node node) {
        this.children.add(node);
    }

    /**
     * @return stringified hash and number of node
     */
    @Override
    public String toString() {
        return String.format("{hash: %s, number: %d, arrivalTime: %s}", hash.toString(), number, arrivalTime);
    }

    //Todo: create tree

    /**
     * Recursively searches for a node with a given hash
     * @param hash hash of the node
     * @return node with the given hash
     */
    public Node getNode(final Hash256 hash) {
        if (this.hash.equals(hash)) {
            return this;
        }
        for (Node child : this.children) {
            Node node = child.getNode(hash);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * Recursively searches for all nodes with a given number
     * @param number number of the block5
     * @return list of nodes with the given number
     */
    public List<Hash256> getNodesWithNumber(final int number) {
        List<Hash256> hashes = new ArrayList<>();
        for (Node child : children) {
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
    public boolean isDescendantOf(final Node parent) {
        if (parent == null) {
            return false;
        }

        if (this.hash.equals(parent.hash)) {
            return true;
        }

        if (parent.children.isEmpty()) {
            return false;
        }

        for (Node child : parent.children) {
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
    public List<Node> getLeaves() {
        List<Node> leaves = new ArrayList<>();
        if (this.children.isEmpty()) {
            leaves.add(this);
        }
        for (Node child : children) {
            leaves.addAll(child.getLeaves());
        }
        return leaves;
    }

    /**
     * Recursively searches for all descendants of the node
     * @return list of the hash of all descendants
     */
    public List<Hash256> getAllDescendants() {
        List<Hash256> desc = new ArrayList<>();
        desc.add(this.hash);
        for (Node child : children) {
            desc.addAll(child.getAllDescendants());
        }
        return desc;
    }

    /**
     * Creates a deep copy of the node of this node
     * @param parent parent of the node
     * @return deep copy of the node
     */
    public Node deepCopy(Node parent) {
        Node copy = new Node(this.hash, parent, new ArrayList<>(), this.number, this.arrivalTime, this.isPrimary);
        for (Node child : children) {
            copy.children.add(child.deepCopy(copy));
        }
        return copy;
    }

    /**
     * Prune the tree by removing all nodes that are not descendants of the finalised node
     * @param finalised finalised node to be pruned
     * @return list of hashes of the pruned nodes
     */
    public List<Hash256> prune(Node finalised) {
        List<Hash256> pruned = new ArrayList<>();
        if (finalised == null) {
            return pruned;
        }

        // if this is a descedent of the finalised block, keep it
        // all descendents of this block will also be descendents of the finalised block,
        // so don't need to check any of those
        if (this.isDescendantOf(finalised)) {
            return pruned;
        }

        // if it's not an ancestor of the finalised block, prune it
        if (!finalised.isDescendantOf(this)) {
            pruned.add(this.hash);
            if (this.parent != null) {
                this.parent.deleteChild(this);
            }
        }

        // if this is an ancestor of the finalised block, keep it,
        // and check its children
        for (Node child : new ArrayList<>(children)) {
            pruned.addAll(child.prune(finalised));
        }

        return pruned;
    }

    /**
     * Deletes a child from the node
     * @param toDelete child to be deleted
     */
    public void deleteChild(Node toDelete) {
        children.removeIf(child -> child.hash.equals(toDelete.hash));
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
