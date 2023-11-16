package com.limechain.storage.block.tree;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.Runtime;
import com.limechain.storage.block.exception.LowerThanRootException;
import com.limechain.storage.block.map.HashToRuntime;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * BlockTree is a tree that represents the current state with all possible blocks
 * that are known to the node. It is used to store the blocks that are received
 * from the network and to be able to retrieve them when needed.
 */
public class BlockTree {

    private final HashToRuntime runtimes;
    @Getter
    private Node root;
    @Getter
    private LeafMap leaves;

    /**
     * Creates a new BlockTree with no blocks. And empty root.
     */
    public BlockTree() {
        this.root = null;
        this.leaves = new LeafMap();
        this.runtimes = new HashToRuntime();
    }

    /**
     * Creates a new BlockTree with a BlockHeader to be set as root.
     *
     * @param root BlockHeader to be set as root node
     */
    public BlockTree(BlockHeader root) {
        final Node n = new Node(
                root.getHash(),
                null,
                new ArrayList<>(),
                root.getBlockNumber().longValueExact(),
                Instant.now(),
                false);
        this.root = n;
        this.leaves = new LeafMap(n);
        this.runtimes = new HashToRuntime();
    }

    /**
     * Adds a new block to the tree.
     * Note: Assumes that the block has no children
     *
     * @param header      BlockHeader to be added
     * @param arrivalTime Arrival time of the block
     */
    public void addBlock(BlockHeader header, Instant arrivalTime) {
        Node parent = getNode(header.getParentHash().getBytes());
        if (parent == null) {
            throw new RuntimeException("Parent does not exist in tree");
        }
        if (getNode(header.getHash()) != null) {
            throw new RuntimeException("Block already exists in tree");
        }

        long number = parent.getNumber() + 1;
        if (number != header.getBlockNumber().longValueExact()) {
            throw new RuntimeException("Block number does not match parent number + 1");
        }

        boolean isPrimary = false;
        if (header.getBlockNumber().longValueExact() != 0) {
            //TODO: Check if primary
        }

        Node newNode = new Node(header.getHash(), parent, new ArrayList<>(), number, arrivalTime, isPrimary);
        parent.addChild(newNode);
        leaves.replace(parent, newNode);
    }

    /**
     * Will return all blocks hashes with the number of the given hash plus one
     * To find all blocks at a number matching a certain block, pass in that block's parent hash
     *
     * @param hash Hash of the block to find the number of
     * @return List of block hashes with the number of the given hash plus one
     */
    public List<byte[]> getAllBlocksAtNumber(byte[] hash) {
        Node node = getNode(hash);
        if (node == null) {
            return new ArrayList<>();
        }

        long number = node.getNumber() + 1;
        if (root.getNumber() == number) {
            return Collections.singletonList(root.getHash());
        }

        return root.getNodesWithNumber(number);
    }

    /**
     * Will return all the blocks between the start and end hash inclusive.
     * If the end hash does not exist in the blocktree then an error is thrown.
     * If the start hash does not exist in the blocktree then we will return all blocks
     * between the end and the blocktree root inclusive
     *
     * @param startHash hash to start from
     * @param endHash   hash to end at
     * @return List of block hashes between the start and end hash inclusive
     */
    public List<byte[]> range(byte[] startHash, byte[] endHash) {
        Node endNode = getNode(endHash);
        if (endNode == null) {
            throw new RuntimeException("End node not found");
        }

        // if we don't find the start hash in the blocktree
        // that means it should be in the database, so we retrieve
        // as many nodes as we can, in other words we get all the
        // blocks from the end hash till the root inclusive
        Node startNode = getNode(startHash);
        if (startNode == null) {
            startNode = root;
        }

        return accumulateHashesInDescendingOrder(endNode, startNode);
    }

    /**
     * Returns the path from the node with Hash start to the node with Hash end.
     * If the end hash does not exist in the blocktree then an error is returned.
     * Different from {@link #range(byte[] startHash, byte[] endHash) range}
     * if the start node is not found in the in memory blocktree then we throw an error.
     *
     * @param startHash hash to start from
     * @param endHash   hash to end at
     * @return List of block hashes between the start and end hash inclusive
     */
    public List<byte[]> rangeInMemory(byte[] startHash, byte[] endHash) {
        Node endNode = getNode(endHash);
        if (endNode == null) {
            throw new RuntimeException("End node not found");
        }

        Node startNode = getNode(startHash);
        if (startNode == null) {
            throw new RuntimeException("Start node not found");
        }

        if (startNode.getNumber() > endNode.getNumber()) {
            throw new RuntimeException("Start is greater than end");
        }

        return accumulateHashesInDescendingOrder(endNode, startNode);
    }

    public List<byte[]> accumulateHashesInDescendingOrder(Node endNode, Node startNode) {
        if (startNode.getNumber() > endNode.getNumber()) {
            throw new RuntimeException("Start is greater than end");
        }

        // blocksInRange is the difference between the end number to start number
        // but the difference don't include the start item that is why we add 1
        int blocksInRange = (int) (endNode.getNumber() - startNode.getNumber() + 1);
        List<byte[]> hashes = new ArrayList<>(blocksInRange);

        for (int position = blocksInRange - 1; position >= 0; position--) {
            hashes.add(endNode.getHash());
            endNode = endNode.getParent();

            if (endNode == null) {
                throw new RuntimeException("End node is null");
            }
        }

        return hashes;
    }

    /**
     * Returns the node with the given hash
     *
     * @param hash Hash of the node to find
     * @return Node with the given hash
     */
    public Node getNode(byte[] hash) {
        if (Arrays.equals(root.getHash(), hash)) {
            return root;
        }

        for (Node leaf : leaves.nodes()) {
            if (Arrays.equals(leaf.getHash(), hash)) {
                return leaf;
            }
        }

        for (Node child : root.getChildren()) {
            Node n = child.getNode(hash);
            if (n != null) {
                return n;
            }
        }

        return null;
    }

    /**
     * Sets the given hash as the new blocktree root, removing all nodes that are not the new root node or
     * its descendant. It returns an array of hashes that have been pruned.
     *
     * @param finalized Hash to prune to
     * @return List of hashes that were pruned
     */
    public List<byte[]> prune(byte[] finalized) {
        if (Arrays.equals(finalized, root.getHash())) {
            return new ArrayList<>();
        }

        Node finalizedNode = getNode(finalized);
        if (finalizedNode == null) {
            return new ArrayList<>();
        }

        Node previousFinalizedBlock = root;
        long newCanonicalChainBlocksCount = finalizedNode.getNumber() - previousFinalizedBlock.getNumber();
        if (previousFinalizedBlock.getNumber() == 0) {
            newCanonicalChainBlocksCount++;
        }

        Node canonicalChainBlock = finalizedNode;
        List<byte[]> newCanonicalChainBlockHashes = new ArrayList<>();
        for (int i = 0; i < newCanonicalChainBlocksCount; i++) {
            newCanonicalChainBlockHashes.add(canonicalChainBlock.getHash());
            canonicalChainBlock = canonicalChainBlock.getParent();
        }

        runtimes.onFinalisation(newCanonicalChainBlockHashes);

        List<byte[]> pruned = root.prune(finalizedNode);
        root = finalizedNode;
        root.setParent(null);

        List<Node> leaves = finalizedNode.getLeaves();
        this.leaves = new LeafMap();
        for (Node leaf : leaves) {
            this.leaves.store(leaf.getHash(), leaf);
        }

        return pruned;
    }

    /**
     * Returns the best node in the block tree using the fork choice rule
     *
     * @return Best block in the block tree
     */
    public Node best() {
        return leaves.bestBlock();
    }

    /**
     * Returns the hash of the block that is considered "best" based on the fork-choice rule.
     * It returns the head of the chain with the most primary blocks. If there are multiple chains with the same number
     * of primaries, it returns the one with the highest head number. If there are multiple chains with the same number
     * of primaries and the same height, it returns the one with the head block that arrived the earliest.
     *
     * @return Hash of the best block
     */
    public byte[] bestBlockHash() {
        if (root.getChildren().isEmpty()) {
            return root.getHash();
        }

        return best().getHash();
    }

    /**
     * Check if child is descendant of parent.
     *
     * @param parent parent hash
     * @param child  child hash
     * @return true if the child is a descendant of parent, false otherwise
     * If parent and child are the same, we return true
     * @throws IllegalArgumentException if either the child or parent are not in the blocktree
     */
    public boolean isDescendantOf(byte[] parent, byte[] child) {
        if (Arrays.equals(parent, child)) {
            return true;
        }

        Node parentNode = getNode(parent);
        if (parentNode == null) {
            throw new IllegalArgumentException("Start node not found: " + new Hash256(parent));
        }

        Node childNode = getNode(child);
        if (childNode == null) {
            throw new IllegalArgumentException("End node not found: " + new Hash256(child));
        }

        return childNode.isDescendantOf(parentNode);
    }

    /**
     * Returns the leaves of the blocktree as a List of hashesh
     *
     * @return List of hashes of the leaves
     */
    public List<byte[]> leaves() {
        return new ArrayList<>(leaves.getSyncMap().keySet());
    }

    /**
     * Find the hash of the lowest common ancestor between block a and b
     *
     * @param a hash of the block a to check
     * @param b hash of the block b to check
     * @return the hash of the lowest common ancestor
     */
    public byte[] lowestCommonAncestor(byte[] a, byte[] b) {
        Node nodeA = getNode(a);
        if (nodeA == null) {
            throw new IllegalArgumentException("Node not found: " + new Hash256(a));
        }

        Node nodeB = getNode(b);
        if (nodeB == null) {
            throw new IllegalArgumentException("Node not found: " + new Hash256(b));
        }

        return lowestCommonAncestor(nodeA, nodeB);
    }

    /**
     * Find the hash of the lowest common ancestor between node a and b
     *
     * @param nodeA Node of block a to check
     * @param nodeB Node of block b to check
     * @return the hash of the lowest common ancestor
     */
    public byte[] lowestCommonAncestor(Node nodeA, Node nodeB) {
        Node higherNode = nodeB;
        Node lowerNode = nodeA;
        if (nodeA.getNumber() > nodeB.getNumber()) {
            higherNode = nodeA;
            lowerNode = nodeB;
        }

        long higherNum = higherNode.getNumber();
        long lowerNum = lowerNode.getNumber();
        long diff = higherNum - lowerNum;
        while (diff > 0) {
            if (higherNode.getParent() == null) {
                throw new IllegalStateException("Out of bounds ancestor check for block number " + higherNum);
            }
            higherNode = higherNode.getParent();
            diff--;
        }

        while (true) {
            if (Arrays.equals(higherNode.getHash(), lowerNode.getHash())) {
                return higherNode.getHash();
            } else if (higherNode.getParent() == null || lowerNode.getParent() == null) {
                throw new IllegalStateException("Out of bounds ancestor check for block number " + higherNum);
            }
            higherNode = higherNode.getParent();
            lowerNode = lowerNode.getParent();
        }
    }

    /**
     * @return List of all blocks in the tree
     */
    public List<byte[]> getAllBlocks() {
        return root.getAllDescendants();
    }

    /**
     * @param hash Hash of the block to find the descendants of
     * @return List of all block hashes that are descendants of the given block hash (including itself).
     */
    public List<byte[]> getAllDescendants(byte[] hash) {
        Node node = getNode(hash);
        if (node == null) {
            throw new IllegalArgumentException("Node not found for block hash " + new Hash256(hash));
        }

        return node.getAllDescendants();
    }

    /**
     * @param num Number of the block to get the hash of
     * @return the block hash with the given number that is on the best chain.
     * If the number is lower or higher than the numbers in the blocktree, an error is returned.
     */
    public byte[] getHashByNumber(long num) {
        Node best = leaves.bestBlock();
        if (best.getNumber() < num) {
            throw new IllegalArgumentException("Number greater than highest");
        }

        if (best.getNumber() == num) {
            return best.getHash();
        }

        if (root.getNumber() > num) {
            throw new LowerThanRootException("Number lower than root");
        }

        if (root.getNumber() == num) {
            return root.getHash();
        }

        Node curr = best.getParent();
        while (true) {
            if (curr == null) {
                throw new IllegalArgumentException("Node not found");
            }

            if (curr.getNumber() == num) {
                return curr.getHash();
            }

            curr = curr.getParent();
        }
    }

    /**
     * Get the arrival time of a block
     *
     * @param hash Hash of the block to get the arrival time of
     * @return Arrival time of the block
     */
    public Instant getArrivalTime(byte[] hash) {
        Node n = getNode(hash);
        if (n == null) {
            throw new IllegalArgumentException("Node not found");
        }

        return n.getArrivalTime();
    }

    /**
     * Creates a deep copy of the block tree
     *
     * @return the deep copy of the block tree
     */
    public BlockTree deepCopy() {
        final BlockTree blockTreeCopy = new BlockTree();

        if (root == null) {
            return blockTreeCopy;
        }

        blockTreeCopy.root = root.deepCopy(null);

        if (leaves != null) {
            blockTreeCopy.leaves = new LeafMap();
            Map<byte[], Node> leafMap = leaves.getSyncMap();
            for (Map.Entry<byte[], Node> entry : leafMap.entrySet()) {
                blockTreeCopy.leaves.store(entry.getKey(), blockTreeCopy.getNode(entry.getValue().getHash()));
            }
        }

        return blockTreeCopy;
    }

    /**
     * Stores the runtime instance for the given block hash
     *
     * @param hash     Block hash
     * @param instance Runtime instance
     */
    public void storeRuntime(byte[] hash, Runtime instance) {
        runtimes.set(hash, instance);
    }

    /**
     * Returns the runtime instance for the given block hash
     *
     * @param hash Block hash
     * @return Runtime instance
     */
    public Runtime getBlockRuntime(byte[] hash) {
        Runtime runtimeInstance = runtimes.get(hash);
        if (runtimeInstance != null) {
            return runtimeInstance;
        }

        Node currentNode = getNode(hash);
        if (currentNode == null) {
            throw new IllegalArgumentException("Node not found for block hash " + hash);
        }

        currentNode = currentNode.getParent();
        while (currentNode != null) {
            runtimeInstance = runtimes.get(currentNode.getHash());
            if (runtimeInstance != null) {
                return runtimeInstance;
            }

            currentNode = currentNode.getParent();
        }

        return null;
    }
}
