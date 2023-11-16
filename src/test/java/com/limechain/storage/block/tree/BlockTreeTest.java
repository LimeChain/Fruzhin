package com.limechain.storage.block.tree;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.runtime.Runtime;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.types.Hash256;
import org.apache.tomcat.util.buf.HexUtils;
import org.javatuples.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BlockTreeTest {

    private static BlockHeader testHeader;

    @BeforeAll
    public static void init() {
        testHeader = new BlockHeader();
        testHeader.setParentHash(getHash("01"));
        testHeader.setBlockNumber(BigInteger.ZERO);
        testHeader.setStateRoot(getHash("02"));
        testHeader.setExtrinsicsRoot(getHash("03"));
        testHeader.setDigest(new HeaderDigest[0]);
    }

    @Test
    void testNewBlockTreeFromNode() {
        BlockTree bt;
        List<BlockNode> branches;

        do {
            Pair<BlockTree, List<BlockNode>> result = createTestBlockTree(testHeader, 5);
            bt = result.getValue0();
            branches = result.getValue1();

        } while (branches.isEmpty() || bt.getNode(branches.get(0).getHash()).getChildren().isEmpty());

        BlockNode testBlockNode = bt.getNode(branches.get(0).getHash()).getChildren().get(0);
        List<BlockNode> leaves = testBlockNode.getLeaves();

//        BlockTree newBt = new BlockTree(testNode);
//        assertEquals(new HashSet<>(leaves), new HashSet<>(newBt.getLeaves().nodes()));
    }

    @Test
    void testBlockTreeGetBlock() {
        Pair<BlockTree, List<byte[]>> result = createFlatTree(2);
        BlockTree bt = result.getValue0();
        List<byte[]> hashes = result.getValue1();

        BlockNode blockNode = bt.getNode(hashes.get(2));
        assertNotNull(blockNode, "node is null");
        assertArrayEquals(hashes.get(2), blockNode.getHash());
    }

    @Test
    void testBlockTreeAddBlock() {
        Pair<BlockTree, List<byte[]>> result = createFlatTree(1);
        BlockTree bt = result.getValue0();
        List<byte[]> hashes = result.getValue1();

        BlockHeader header = createHeader(hashes.get(1), 2);
        byte[] hash = header.getHash();
        bt.addBlock(header, Instant.ofEpochSecond(0));

        BlockNode blockNode = bt.getNode(hash);
        BlockNode leafBlockNode = bt.getLeaves().load(blockNode.getHash());

        assertNotNull(leafBlockNode);

        byte[] oldHash = new byte[]{0x01};
        leafBlockNode = bt.getLeaves().load(oldHash);
        assertNull(leafBlockNode);
    }

    @Test
    void testNodeIsDescendantOf() {
        Pair<BlockTree, List<byte[]>> result = createFlatTree(4);
        BlockTree bt = result.getValue0();
        List<byte[]> hashes = result.getValue1();

        BlockNode leaf = bt.getNode(hashes.get(3));
        assertTrue(leaf.isDescendantOf(bt.getRoot()));

        assertFalse(bt.getRoot().isDescendantOf(leaf));
    }

    @Test
    void testBlockTreeGetNode() {
        Pair<BlockTree, List<BlockNode>> result = createTestBlockTree(testHeader, 16);
        BlockTree bt = result.getValue0();
        List<BlockNode> branches = result.getValue1();

        for (BlockNode branch : branches) {
            BlockHeader header = createHeader(branch.getHash(), branch.getNumber() + 1, getHash("02"));
            bt.addBlock(header, Instant.ofEpochSecond(0));
        }

        BlockNode block = bt.getNode(branches.get(0).getHash());
        assertNotNull(block, "Block should not be null");
    }

    @Test
    void testBlockTreeGetAllDescendants() {
        Pair<BlockTree, List<byte[]>> result = createFlatTree(4);
        BlockTree bt = result.getValue0();
        List<Hash256> hashes = result.getValue1().stream().map(Hash256::new).collect(Collectors.toList());
        List<Hash256> descendants = bt.getAllDescendants(bt.getRoot().getHash())
                .stream().map(Hash256::new).collect(Collectors.toList());

        assertEquals(hashes, descendants);
    }

    @Test
    void testBlockTreeIsDescendantOf() {
        Pair<BlockTree, List<byte[]>> result = createFlatTree(4);
        BlockTree bt = result.getValue0();
        List<byte[]> hashes = result.getValue1();

        boolean isDescendant = bt.isDescendantOf(bt.getRoot().getHash(), hashes.get(3));
        assertTrue(isDescendant);

        isDescendant = bt.isDescendantOf(hashes.get(3), bt.getRoot().getHash());
        assertFalse(isDescendant);
    }

    @Test
    void testBlockTreeLowestCommonAncestor() {
        BlockTree bt;
        List<byte[]> leaves;
        List<BlockNode> branches;

        while (true) {
            Pair<BlockTree, List<BlockNode>> result = createTestBlockTree(testHeader, 8);
            bt = result.getValue0();
            branches = result.getValue1();
            leaves = bt.leaves();

            if (leaves.size() == 2) {
                break;
            }
        }

        byte[] expected = branches.get(0).getHash();
        byte[] a = leaves.get(0);
        byte[] b = leaves.get(1);

        byte[] p = bt.lowestCommonAncestor(a, b);
        assertArrayEquals(expected, p);
    }

    @Test
    void testBlockTreeLowestCommonAncestorSameNode() {
        Pair<BlockTree, List<BlockNode>> result = createTestBlockTree(testHeader, 8);
        BlockTree bt = result.getValue0();
        List<byte[]> leaves = bt.leaves();

        byte[] a = leaves.get(0);

        byte[] p = bt.lowestCommonAncestor(a, a);
        assertEquals(a, p, "Lowest common ancestor of a node with itself should be itself");
    }

    @Test
    void testBlockTreeLowestCommonAncestorSameChain() {
        Pair<BlockTree, List<BlockNode>> result = createTestBlockTree(testHeader, 8);
        BlockTree bt = result.getValue0();
        List<byte[]> leaves = bt.leaves();

        byte[] a = leaves.get(0);
        byte[] b = bt.getNode(a).getParent().getHash();

        byte[] p = bt.lowestCommonAncestor(a, b);
        assertEquals(b, p, "Lowest common ancestor should be b as it is a's parent");
    }

    @Test
    public void testPruneWhenFinalisedHashIsRootHash() {
        Pair<BlockTree, List<byte[]>> flatTreePair = createFlatTree(1);
        BlockTree bt = flatTreePair.getValue0();

        List<byte[]> pruned = bt.prune(bt.getRoot().getHash());
        assertTrue(pruned.isEmpty(), "Pruned list should be empty");
    }

    @Test
    void testPruneWhenNodeNotFound() {
        Pair<BlockTree, List<byte[]>> flatTreePair = createFlatTree(0);
        BlockTree bt = flatTreePair.getValue0();

        List<byte[]> pruned = bt.prune(getHash("01").getBytes());
        assertTrue(pruned.isEmpty(), "Pruned list should be empty when node is not found");
    }

    @Test
    void testPruneNothingToPrune() {
        BlockHeader rootHeader = createHeader(getHash("01").getBytes(), 0);
        BlockTree blockTree = new BlockTree(rootHeader);
        BlockNode rootBlockNode = blockTree.getRoot();

        BlockNode childBlockNode = new BlockNode(getHash("02").getBytes(), rootBlockNode, new ArrayList<>(),
                1, null, false);
        rootBlockNode.addChild(childBlockNode);
        blockTree.getLeaves().replace(rootBlockNode, childBlockNode);

        List<byte[]> pruned = blockTree.prune(getHash("02").getBytes());
        assertTrue(pruned.isEmpty(), "Pruned list should be empty when there's nothing to prune");
    }

    @Test
    void testPruneCanonicalRuntimes() {
        BlockHeader rootHeader = createHeader(getHash("01").getBytes(), 0);

        BlockTree blockTree = new BlockTree(rootHeader);
        BlockNode rootBlockNode = blockTree.getRoot();

        Runtime rootRuntime = mock(Runtime.class);
        blockTree.storeRuntime(getHash("01").getBytes(), rootRuntime);

        BlockNode childBlockNode = new BlockNode(getHash("02").getBytes(), rootBlockNode, new ArrayList<>(),
                1, null, false);
        rootBlockNode.addChild(childBlockNode);
        blockTree.getLeaves().replace(rootBlockNode, childBlockNode);

        Runtime leafRuntime = mock(Runtime.class);
        blockTree.storeRuntime(getHash("02").getBytes(), leafRuntime);

        List<byte[]> pruned = blockTree.prune(getHash("02").getBytes());
        assertTrue(pruned.isEmpty());
    }

    @Test
    void testPruneFork() {
        BlockHeader rootHeader = createHeader(getHash("01").getBytes(), 0);
        BlockTree blockTree = new BlockTree(rootHeader);
        BlockNode rootBlockNode = blockTree.getRoot();

        // Set runtime for root
        Runtime rootRuntime = Mockito.mock(Runtime.class);
        blockTree.storeRuntime(getHash("01").getBytes(), rootRuntime);

        // Add child node {1} -> {2}
        BlockNode childBlockNode2 = new BlockNode(getHash("02").getBytes(), rootBlockNode, 1);
        rootBlockNode.addChild(childBlockNode2);
        blockTree.getLeaves().replace(rootBlockNode, childBlockNode2);

        // Add another child node {1} -> {3}
        BlockNode childBlockNode3 = new BlockNode(getHash("03").getBytes(), rootBlockNode, 1);
        rootBlockNode.addChild(childBlockNode3);
        blockTree.getLeaves().replace(rootBlockNode, childBlockNode3);

        // Set runtime to be pruned
        Runtime runtimeToBePruned = Mockito.mock(Runtime.class);
        blockTree.storeRuntime(getHash("03").getBytes(), runtimeToBePruned);

        // Perform pruning
        List<byte[]> pruned = blockTree.prune(getHash("02").getBytes());

        // Verify that node {3} is pruned
        assertArrayEquals(getHash("03").getBytes(), pruned.get(0));

        // Asserting the runtime mapping
//        assertEquals(rootRuntime, blockTree.getBlockRuntime(getHash("02").getBytes()));
        //TODO: currently broken
    }

    /*Helper methods*/
    private static Hash256 getHash(String hash) {
        byte[] bytes = HashUtils.hashWithBlake2b(HexUtils.fromHexString(hash));
        return new Hash256(bytes);
    }

    private Pair<BlockTree, List<BlockNode>> createTestBlockTree(BlockHeader header, int number) {
        BlockTree bt = new BlockTree(header);
        byte[] previousHash = header.getHash();

        List<BlockNode> branches = new ArrayList<>();
        Random r = new Random();

        long at = 0;

        for (int i = 1; i <= number; i++) {
            BlockHeader newHeader = createHeader(previousHash, i);
            byte[] hash = newHeader.getHash();

            try {
                bt.addBlock(newHeader, Instant.ofEpochSecond(0, at));
            } catch (Exception e) {
                throw new RuntimeException("Error adding block: " + e.getMessage());
            }

            previousHash = hash;
            boolean isBranch = r.nextBoolean();
            if (isBranch) {
                branches.add(new BlockNode(hash, null, new ArrayList<>(), bt.getNode(hash).getNumber(),
                        Instant.ofEpochSecond(0, at), false));
            }

            at += r.nextInt(8);
        }

        for (BlockNode branch : branches) {
            at = branch.getArrivalTime().getEpochSecond();
            previousHash = branch.getHash();

            for (long i = branch.getNumber(); i <= number; i++) {
                BlockHeader branchHeader = createHeader(previousHash, i + 1);
                branchHeader.setStateRoot(getHash("01"));
                byte[] branchHash = branchHeader.getHash();

                try {
                    bt.addBlock(branchHeader, Instant.ofEpochSecond(0, at));
                } catch (Exception e) {
                    throw new RuntimeException("Error adding block: " + e.getMessage());
                }

                previousHash = branchHash;
                at += r.nextInt(8);
            }
        }

        return new Pair<>(bt, branches);
    }

    private Pair<BlockTree, List<byte[]>> createFlatTree(int number) {
        BlockHeader rootHeader = createHeader(getHash("00").getBytes(), 0, getHash("00"));

        BlockTree bt = new BlockTree(rootHeader);

        List<byte[]> hashes = new ArrayList<>();
        hashes.add(bt.getRoot().getHash());

        byte[] previousHash = bt.getRoot().getHash();

        for (int i = 1; i <= number; i++) {
            BlockHeader header = createHeader(previousHash, i, getHash("00"));
            header.setParentHash(new Hash256(previousHash));
            header.setBlockNumber(BigInteger.valueOf(i));
            header.setStateRoot(getHash("00"));
            header.setExtrinsicsRoot(getHash("00"));
            header.setDigest(new HeaderDigest[0]);

            byte[] hash = header.getHash();
            hashes.add(hash);

            try {
                bt.addBlock(header, Instant.ofEpochSecond(0));
            } catch (Exception e) {
                throw new RuntimeException("Error adding block: " + e.getMessage());
            }

            previousHash = hash;
        }

        return new Pair<>(bt, hashes);
    }

    private BlockHeader createHeader(byte[] previousHash, long i, Hash256 hash) {
        BlockHeader rootHeader = new BlockHeader();
        rootHeader.setParentHash(new Hash256(previousHash));
        rootHeader.setStateRoot(hash);
        rootHeader.setExtrinsicsRoot(hash);
        rootHeader.setDigest(new HeaderDigest[0]);
        rootHeader.setBlockNumber(BigInteger.valueOf(i));
        return rootHeader;
    }

    private BlockHeader createHeader(byte[] previousHash, long i) {
        return createHeader(previousHash, i, getHash("00"));
    }

}