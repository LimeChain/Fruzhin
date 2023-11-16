package com.limechain.storage.block.tree;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.Runtime;
import com.limechain.storage.block.exception.BlockAlreadyExistsException;
import com.limechain.storage.block.exception.BlockNodeNotFoundException;
import com.limechain.storage.block.exception.BlockStorageGenericException;
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
    private BlockNode root;
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
        final BlockNode n = new BlockNode(
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
     * @throws BlockNodeNotFoundException   if the parent of the block does not exist in the tree.
     * @throws BlockAlreadyExistsException  if a block with the same hash already exists in the tree.
     * @throws BlockStorageGenericException if the block number does not match the expected value (parent number + 1).
     */
    public void addBlock(BlockHeader header, Instant arrivalTime) {
        BlockNode parent = getNode(header.getParentHash().getBytes());
        if (parent == null) {
            throw new BlockNodeNotFoundException("Parent does not exist in tree");
        }
        if (getNode(header.getHash()) != null) {
            throw new BlockAlreadyExistsException("Block already exists in tree");
        }

        long number = parent.getNumber() + 1;
        if (number != header.getBlockNumber().longValueExact()) {
            throw new BlockStorageGenericException("Block number does not match parent number + 1");
        }

        boolean isPrimary = false;
        if (header.getBlockNumber().longValueExact() != 0) {
            //TODO: Check if primary
        }

        BlockNode
                newBlockNode = new BlockNode(header.getHash(), parent, new ArrayList<>(),
                number, arrivalTime, isPrimary);
        parent.addChild(newBlockNode);
        leaves.replace(parent, newBlockNode);
    }

    /**
     * Will return all blocks hashes with the number of the given hash plus one
     * To find all blocks at a number matching a certain block, pass in that block's parent hash
     *
     * @param hash Hash of the block to find the number of
     * @return List of block hashes with the number of the given hash plus one
     */
    public List<byte[]> getAllBlocksAtNumber(byte[] hash) {
        BlockNode blockNode = getNode(hash);
        if (blockNode == null) {
            return new ArrayList<>();
        }

        long number = blockNode.getNumber() + 1;
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
     * @throws BlockNodeNotFoundException if the end hash does not exist in the blocktree
     * @throws IllegalArgumentException     if the startHash is greater than the endHash
     * @throws BlockStorageGenericException if there is no node between start and end node during traversal.
     */
    public List<byte[]> range(byte[] startHash, byte[] endHash) {
        BlockNode endBlockNode = getNode(endHash);
        if (endBlockNode == null) {
            throw new BlockNodeNotFoundException("End node not found");
        }

        // if we don't find the start hash in the blocktree
        // that means it should be in the database, so we retrieve
        // as many nodes as we can, in other words we get all the
        // blocks from the end hash till the root inclusive
        BlockNode startBlockNode = getNode(startHash);
        if (startBlockNode == null) {
            startBlockNode = root;
        }

        return accumulateHashesInDescendingOrder(endBlockNode, startBlockNode);
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
     * @throws BlockNodeNotFoundException   if either the start or end node is not found in the blocktree.
     * @throws BlockStorageGenericException if the start node's number is greater than the end node's number.
     */
    public List<byte[]> rangeInMemory(byte[] startHash, byte[] endHash) {
        BlockNode endBlockNode = getNode(endHash);
        if (endBlockNode == null) {
            throw new BlockNodeNotFoundException("End node not found");
        }

        BlockNode startBlockNode = getNode(startHash);
        if (startBlockNode == null) {
            throw new BlockNodeNotFoundException("Start node not found");
        }

        if (startBlockNode.getNumber() > endBlockNode.getNumber()) {
            throw new BlockStorageGenericException("Start is greater than end");
        }

        return accumulateHashesInDescendingOrder(endBlockNode, startBlockNode);
    }

    /**
     * Accumulates the hashes of block nodes in descending order from the end block node to the start block node.
     *
     * @param endBlockNode   The block node from which to start accumulating hashes.
     * @param startBlockNode The block node at which to stop accumulating hashes.
     * @return A list of byte arrays representing the hashes of the block nodes, in descending order from end to start.
     * @throws IllegalArgumentException     if the start node's number is greater than the end node's number
     * @throws BlockStorageGenericException if there is no node between start and end node during traversal.
     */
    public List<byte[]> accumulateHashesInDescendingOrder(BlockNode endBlockNode, BlockNode startBlockNode) {
        if (startBlockNode.getNumber() > endBlockNode.getNumber()) {
            throw new IllegalArgumentException("Start is greater than end");
        }

        // blocksInRange is the difference between the end number to start number
        // but the difference don't include the start item that is why we add 1
        int blocksInRange = (int) (endBlockNode.getNumber() - startBlockNode.getNumber() + 1);
        List<byte[]> hashes = new ArrayList<>(blocksInRange);

        for (int position = blocksInRange - 1; position >= 0; position--) {
            hashes.add(endBlockNode.getHash());
            endBlockNode = endBlockNode.getParent();

            if (endBlockNode == null) {
                throw new BlockStorageGenericException("End node is null");
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
    public BlockNode getNode(byte[] hash) {
        if (Arrays.equals(root.getHash(), hash)) {
            return root;
        }

        for (BlockNode leaf : leaves.nodes()) {
            if (Arrays.equals(leaf.getHash(), hash)) {
                return leaf;
            }
        }

        for (BlockNode child : root.getChildren()) {
            BlockNode n = child.getNode(hash);
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

        BlockNode finalizedBlockNode = getNode(finalized);
        if (finalizedBlockNode == null) {
            return new ArrayList<>();
        }

        BlockNode previousFinalizedBlock = root;
        long newCanonicalChainBlocksCount = finalizedBlockNode.getNumber() - previousFinalizedBlock.getNumber();
        if (previousFinalizedBlock.getNumber() == 0) {
            newCanonicalChainBlocksCount++;
        }

        BlockNode canonicalChainBlock = finalizedBlockNode;
        List<byte[]> newCanonicalChainBlockHashes = new ArrayList<>();
        for (int i = 0; i < newCanonicalChainBlocksCount; i++) {
            newCanonicalChainBlockHashes.add(canonicalChainBlock.getHash());
            canonicalChainBlock = canonicalChainBlock.getParent();
        }

        runtimes.onFinalisation(newCanonicalChainBlockHashes);

        List<byte[]> pruned = root.prune(finalizedBlockNode);
        root = finalizedBlockNode;
        root.setParent(null);

        List<BlockNode> leaves = finalizedBlockNode.getLeaves();
        this.leaves = new LeafMap();
        for (BlockNode leaf : leaves) {
            this.leaves.store(leaf.getHash(), leaf);
        }

        return pruned;
    }

    /**
     * Returns the best node in the block tree using the fork choice rule
     *
     * @return Best block in the block tree
     */
    public BlockNode best() {
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
     * @throws BlockNodeNotFoundException if either the child or parent are not in the blocktree
     */
    public boolean isDescendantOf(byte[] parent, byte[] child) {
        if (Arrays.equals(parent, child)) {
            return true;
        }

        BlockNode parentBlockNode = getNode(parent);
        if (parentBlockNode == null) {
            throw new BlockNodeNotFoundException("Start node not found: " + new Hash256(parent));
        }

        BlockNode childBlockNode = getNode(child);
        if (childBlockNode == null) {
            throw new BlockNodeNotFoundException("End node not found: " + new Hash256(child));
        }

        return childBlockNode.isDescendantOf(parentBlockNode);
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
     * @param a hash of the first block to check
     * @param b hash of the second block to check
     * @return the hash of the lowest common ancestor
     * @throws IllegalArgumentException if either node corresponding to hash a or hash b is not found.
     */
    public byte[] lowestCommonAncestor(byte[] a, byte[] b) {
        BlockNode blockNodeA = getNode(a);
        if (blockNodeA == null) {
            throw new IllegalArgumentException("Node not found: " + new Hash256(a));
        }

        BlockNode blockNodeB = getNode(b);
        if (blockNodeB == null) {
            throw new IllegalArgumentException("Node not found: " + new Hash256(b));
        }

        return lowestCommonAncestor(blockNodeA, blockNodeB);
    }

    /**
     * Find the hash of the lowest common ancestor between node a and b
     *
     * @param blockNodeA Node of first block to check
     * @param blockNodeB Node of second block to check
     * @return the hash of the lowest common ancestor
     * @throws BlockStorageGenericException if an ancestor check goes out of bounds,
     * indicating that one of the nodes does not have a parent in the chain.
     */
    public byte[] lowestCommonAncestor(BlockNode blockNodeA, BlockNode blockNodeB) {
        BlockNode higherBlockNode = blockNodeB;
        BlockNode lowerBlockNode = blockNodeA;
        if (blockNodeA.getNumber() > blockNodeB.getNumber()) {
            higherBlockNode = blockNodeA;
            lowerBlockNode = blockNodeB;
        }

        long higherNum = higherBlockNode.getNumber();
        long lowerNum = lowerBlockNode.getNumber();
        long diff = higherNum - lowerNum;
        while (diff > 0) {
            if (higherBlockNode.getParent() == null) {
                throw new BlockStorageGenericException("Out of bounds ancestor check for block number " + higherNum);
            }
            higherBlockNode = higherBlockNode.getParent();
            diff--;
        }

        while (true) {
            if (Arrays.equals(higherBlockNode.getHash(), lowerBlockNode.getHash())) {
                return higherBlockNode.getHash();
            } else if (higherBlockNode.getParent() == null || lowerBlockNode.getParent() == null) {
                throw new BlockStorageGenericException("Out of bounds ancestor check for block number " + higherNum);
            }
            higherBlockNode = higherBlockNode.getParent();
            lowerBlockNode = lowerBlockNode.getParent();
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
     * @throws BlockNodeNotFoundException if no node is found for the given block hash.
     */
    public List<byte[]> getAllDescendants(byte[] hash) {
        BlockNode blockNode = getNode(hash);
        if (blockNode == null) {
            throw new BlockNodeNotFoundException("Node not found for block hash " + new Hash256(hash));
        }

        return blockNode.getAllDescendants();
    }

    /**
     * @param num Number of the block to get the hash of
     * @return the block hash with the given number that is on the best chain.
     * If the number is lower or higher than the numbers in the blocktree, an error is returned.
     * @throws BlockStorageGenericException if the block number is greater than the highest number in the blocktree.
     * @throws LowerThanRootException if the block number is lower than the root of the blocktree.
     * @throws BlockNodeNotFoundException if no node with the given number is found in the blocktree.
     */
    public byte[] getHashByNumber(long num) {
        BlockNode best = leaves.bestBlock();
        if (best.getNumber() < num) {
            throw new BlockStorageGenericException("Number greater than highest");
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

        BlockNode curr = best.getParent();
        while (true) {
            if (curr == null) {
                throw new BlockNodeNotFoundException("Node not found");
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
     * @throws BlockNodeNotFoundException if no node is found for the given block hash.
     * @return Arrival time of the block
     */
    public Instant getArrivalTime(byte[] hash) {
        BlockNode n = getNode(hash);
        if (n == null) {
            throw new BlockNodeNotFoundException("Node not found");
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
            Map<byte[], BlockNode> leafMap = leaves.getSyncMap();
            for (Map.Entry<byte[], BlockNode> entry : leafMap.entrySet()) {
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
     * @throws BlockNodeNotFoundException if no node is found for the given block hash.
     */
    public Runtime getBlockRuntime(byte[] hash) {
        Runtime runtimeInstance = runtimes.get(hash);
        if (runtimeInstance != null) {
            return runtimeInstance;
        }

        BlockNode currentBlockNode = getNode(hash);
        if (currentBlockNode == null) {
            throw new BlockNodeNotFoundException("Node not found for block hash " + new Hash256(hash));
        }

        currentBlockNode = currentBlockNode.getParent();
        while (currentBlockNode != null) {
            runtimeInstance = runtimes.get(currentBlockNode.getHash());
            if (runtimeInstance != null) {
                return runtimeInstance;
            }

            currentBlockNode = currentBlockNode.getParent();
        }

        return null;
    }
}
