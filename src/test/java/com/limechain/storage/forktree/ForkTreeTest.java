package com.limechain.storage.forktree;

import com.limechain.exception.forktree.ForkTreeException;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForkTreeTest {

    private static final BiPredicate<Hash256, Hash256> FALSE_BIPREDICATE = (a, b) -> false;
    private static final BiPredicate<Hash256, Hash256> TRUE_BIPREDICATE = (a, b) -> true;
    private static final Integer DATA = 42;

    private ForkTree<Integer> tree;

    @BeforeEach
    void setup() {
        tree = new ForkTree<>();
    }

    @Test
    void testImportNodeAsRoot() throws Exception {
        Hash256 hash = new Hash256(generateHash(1));
        boolean isRoot = tree.importNode(hash, BigInteger.ONE, DATA, FALSE_BIPREDICATE);

        assertTrue(isRoot);
        assertEquals(1, tree.getRoots().size());
        assertEquals(hash, tree.getRoots().get(0).getHash());
    }

    @Test
    void testImportRootNodeAndChildNode() throws Exception {
        Hash256 hashA = new Hash256(generateHash(1));
        Hash256 hashB = new Hash256(generateHash(2));

        boolean isRoot = tree.importNode(hashA, BigInteger.TEN, DATA, FALSE_BIPREDICATE);
        assertTrue(isRoot);
        assertEquals(1, tree.getRoots().size());
        assertEquals(hashA, tree.getRoots().get(0).getHash());

        isRoot = tree.importNode(hashB, BigInteger.valueOf(20), DATA, TRUE_BIPREDICATE);
        assertFalse(isRoot);
        assertEquals(1, tree.getRoots().get(0).getChildren().size());
        assertEquals(hashB, tree.getRoots().get(0).getChildren().get(0).getHash());
    }

    @Test
    void testImportNodeWithBlockNumberSmallerThanBestFinalizedBlockShouldThrowRevertException() {
        tree.setBestFinalizedNumber(BigInteger.TEN);

        Hash256 hashA = new Hash256(generateHash(1));

        assertThrows(ForkTreeException.class,
                () -> tree.importNode(hashA, BigInteger.ONE, 1, FALSE_BIPREDICATE));
    }

    @Test
    void testImportNodeDuplicateInRoots() throws Exception {
        Hash256 hash = new Hash256(generateHash(1));
        tree.importNode(hash, BigInteger.ONE, DATA, FALSE_BIPREDICATE);

        assertThrows(ForkTreeException.class, () ->
                tree.importNode(hash, BigInteger.valueOf(1), DATA, FALSE_BIPREDICATE));
    }

    @Test
    void testImportNodeDuplicateInChildren() throws Exception {
        Hash256 hashRoot = new Hash256(generateHash(1));

        tree.importNode(hashRoot, BigInteger.ONE, DATA, FALSE_BIPREDICATE);

        Hash256 hashChild = new Hash256(generateHash(2));
        tree.importNode(hashChild, BigInteger.TWO, DATA, TRUE_BIPREDICATE);

        assertThrows(ForkTreeException.class, () ->
                tree.importNode(hashChild, BigInteger.TWO, DATA, TRUE_BIPREDICATE));
    }

    @Test
    void testImportNodeMultiplePotentialParents() throws Exception {
        // |A| -> B -> D
        // |C|
        Hash256 hashA = new Hash256(generateHash(1));
        boolean isANodeRoot = tree.importNode(hashA, BigInteger.TEN, DATA, FALSE_BIPREDICATE);

        Hash256 hashB = new Hash256(generateHash(2));
        boolean isBNodeRoot = tree.importNode(hashB, BigInteger.valueOf(20), DATA, TRUE_BIPREDICATE);

        Hash256 hashC = new Hash256(generateHash(3));
        boolean isCNodeRoot = tree.importNode(hashC, BigInteger.valueOf(15), DATA, FALSE_BIPREDICATE);

        Hash256 hashD = new Hash256(generateHash(4));
        boolean isDNodeRoot = tree.importNode(hashD, BigInteger.valueOf(30), DATA, TRUE_BIPREDICATE);

        assertTrue(isANodeRoot);
        assertFalse(isBNodeRoot);
        assertTrue(isCNodeRoot);
        assertFalse(isDNodeRoot);

        ForkTree.ForkTreeNode<Integer> aNode = tree.getRoots().get(0);
        assertEquals(hashA, aNode.getHash());
        List<ForkTree.ForkTreeNode<Integer>> aChildren = aNode.getChildren();
        assertEquals(1, aChildren.size());
        ForkTree.ForkTreeNode<Integer> aChild = aChildren.get(0);
        assertEquals(hashB, aChild.getHash());
        assertEquals(1, aChild.getChildren().size());

        ForkTree.ForkTreeNode<Integer> cNode = tree.getRoots().get(1);
        assertEquals(hashC, cNode.getHash());
        List<ForkTree.ForkTreeNode<Integer>> cChildren = cNode.getChildren();
        assertEquals(0, cChildren.size());
    }

    @Test
    void testRebalanceSortsRoots() throws Exception {
        // |B|
        // |A| - C
        ForkTree.ForkTreeNode<Integer> nodeA =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(1)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeB =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(2)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeC =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(3)), BigInteger.ONE, DATA);

        nodeA.getChildren().add(nodeC);
        tree.getRoots().add(nodeB);
        tree.getRoots().add(nodeA);

        callRebalance();

        assertEquals(nodeA.getHash(), tree.getRoots().get(0).getHash());
        assertEquals(nodeB.getHash(), tree.getRoots().get(1).getHash());
    }

    @Test
    void testRebalanceSortsChildren() throws Exception {
        // |H| - D
        // |A| - B - E - G
        //  | -> C - F
        ForkTree.ForkTreeNode<Integer> nodeA =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(1)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeB =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(2)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeC =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(3)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeD =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(4)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeE =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(5)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeF =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(6)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeG =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(7)), BigInteger.ONE, DATA);
        ForkTree.ForkTreeNode<Integer> nodeH =
                new ForkTree.ForkTreeNode<>(new Hash256(generateHash(8)), BigInteger.ONE, DATA);

        nodeE.getChildren().add(nodeG);
        nodeB.getChildren().add(nodeE);
        nodeA.getChildren().add(nodeB);

        nodeC.getChildren().add(nodeF);
        nodeA.getChildren().add(nodeC);

        nodeH.getChildren().add(nodeD);

        tree.getRoots().add(nodeH);
        tree.getRoots().add(nodeA);

        callRebalance();

        assertEquals(nodeA.getHash(), tree.getRoots().get(0).getHash());
        assertEquals(nodeB.getHash(), tree.getRoots().get(0).getChildren().get(0).getHash());
        assertEquals(nodeC.getHash(), tree.getRoots().get(0).getChildren().get(1).getHash());
        assertEquals(nodeH.getHash(), tree.getRoots().get(1).getHash());
    }

    @Test
    void testIteratorBFS() throws Exception {
        Hash256 hashA = new Hash256(generateHash(1));
        Hash256 hashB = new Hash256(generateHash(2));
        Hash256 hashC = new Hash256(generateHash(3));

        // |A|
        // |B|
        // |C|
        tree.importNode(hashA, BigInteger.TEN, 1, FALSE_BIPREDICATE);
        tree.importNode(hashB, BigInteger.valueOf(20), 2, FALSE_BIPREDICATE);
        tree.importNode(hashC, BigInteger.valueOf(30), 3, FALSE_BIPREDICATE);

        Iterator<Integer> it = callIterator();

        List<Integer> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        assertArrayEquals(new Integer[]{1, 2, 3}, result.toArray(new Integer[0]));
    }

    @Test
    void testIteratorWithForkTreeHavingBranches() throws Exception {
        Hash256 hashA = new Hash256(generateHash(1));
        Hash256 hashB = new Hash256(generateHash(2));
        Hash256 hashC = new Hash256(generateHash(3));
        Hash256 hashD = new Hash256(generateHash(4));
        Hash256 hashE = new Hash256(generateHash(5));

        // |A| -> B -> C -> E
        // |C|
        tree.importNode(hashA, BigInteger.TEN, 1, FALSE_BIPREDICATE);
        tree.importNode(hashB, BigInteger.valueOf(20), 2, TRUE_BIPREDICATE);
        tree.importNode(hashC, BigInteger.valueOf(30), 3, FALSE_BIPREDICATE);
        tree.importNode(hashD, BigInteger.valueOf(40), 4, TRUE_BIPREDICATE);
        tree.importNode(hashE, BigInteger.valueOf(50), 5, TRUE_BIPREDICATE);

        Iterator<Integer> it = callIterator();
        List<Integer> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        assertArrayEquals(new Integer[]{1, 3, 2, 4, 5}, result.toArray(new Integer[0]));
    }

    @Test
    void testFindParentForInsertionNoCandidate()
            throws Exception {
        Hash256 hashA = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> nodeA = new ForkTree.ForkTreeNode<>(hashA, BigInteger.valueOf(10), DATA);
        tree.getRoots().add(nodeA);

        ForkTree.ForkTreeNode<Integer> result =
                callFindParentForInsertion(new Hash256(generateHash(2)), BigInteger.valueOf(5), TRUE_BIPREDICATE);

        assertNull(result);
    }

    @Test
    void testFindParentForInsertionSingleLevelCandidate()
            throws Exception {
        Hash256 hashA = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> nodeA = new ForkTree.ForkTreeNode<>(hashA, BigInteger.valueOf(10), DATA);
        tree.getRoots().add(nodeA);

        ForkTree.ForkTreeNode<Integer> result =
                callFindParentForInsertion(new Hash256(generateHash(2)), BigInteger.valueOf(20), TRUE_BIPREDICATE);

        assertEquals(nodeA, result);
    }

    @Test
    void testFindParentForInsertionMultipleLevels()
            throws Exception {
        
        // |A| - B - C - new
        Hash256 hashA = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> nodeA = new ForkTree.ForkTreeNode<>(hashA, BigInteger.valueOf(10), DATA);
        tree.getRoots().add(nodeA);

        Hash256 hashB = new Hash256(generateHash(2));
        ForkTree.ForkTreeNode<Integer> nodeB = new ForkTree.ForkTreeNode<>(hashB, BigInteger.valueOf(15), DATA);
        nodeA.getChildren().add(nodeB);

        Hash256 hashC = new Hash256(generateHash(3));
        ForkTree.ForkTreeNode<Integer> nodeC = new ForkTree.ForkTreeNode<>(hashC, BigInteger.valueOf(18), DATA);
        nodeB.getChildren().add(nodeC);

        Hash256 hashNew = new Hash256(generateHash(4));
        ForkTree.ForkTreeNode<Integer> result =
                callFindParentForInsertion(hashNew, BigInteger.valueOf(20), TRUE_BIPREDICATE);

        assertEquals(nodeC, result);
    }

    @Test
    void testFindParentForInsertionCreatingFork() throws Exception {
        // |A| - B - C
        //       | - new
        Hash256 hashA = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> nodeA = new ForkTree.ForkTreeNode<>(hashA, BigInteger.valueOf(10), DATA);
        tree.getRoots().add(nodeA);

        Hash256 hashB = new Hash256(generateHash(2));
        ForkTree.ForkTreeNode<Integer> nodeB = new ForkTree.ForkTreeNode<>(hashB, BigInteger.valueOf(15), DATA);
        nodeA.getChildren().add(nodeB);

        Hash256 hashC = new Hash256(generateHash(3));
        ForkTree.ForkTreeNode<Integer> nodeC = new ForkTree.ForkTreeNode<>(hashC, BigInteger.valueOf(18), DATA);
        nodeB.getChildren().add(nodeC);

        Hash256 hashNew = new Hash256(generateHash(4));
        ForkTree.ForkTreeNode<Integer> result =
                callFindParentForInsertion(hashNew, BigInteger.valueOf(18), TRUE_BIPREDICATE);

        assertEquals(nodeB, result);
    }

    @Test
    void testFindParentForInsertionCreatingNewRoot() throws Exception {
        // |A| - B - C
        // |new|
        Hash256 hashA = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> nodeA = new ForkTree.ForkTreeNode<>(hashA, BigInteger.valueOf(10), DATA);
        tree.getRoots().add(nodeA);

        Hash256 hashB = new Hash256(generateHash(2));
        ForkTree.ForkTreeNode<Integer> nodeB = new ForkTree.ForkTreeNode<>(hashB, BigInteger.valueOf(15), DATA);
        nodeA.getChildren().add(nodeB);

        Hash256 hashC = new Hash256(generateHash(3));
        ForkTree.ForkTreeNode<Integer> nodeC = new ForkTree.ForkTreeNode<>(hashC, BigInteger.valueOf(18), DATA);
        nodeB.getChildren().add(nodeC);

        Hash256 hashNew = new Hash256(generateHash(4));
        ForkTree.ForkTreeNode<Integer> result =
                callFindParentForInsertion(hashNew, BigInteger.valueOf(10), TRUE_BIPREDICATE);

        assertNull(result);
    }

    @Test
    void testFindParentForInsertionCreatingForkInTreeWithMultipleForks() throws Exception {
        // |A| - B - D - E
        //       |   | - F
        //       |   | - new
        //       | - C
        Hash256 hashA = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> nodeA = new ForkTree.ForkTreeNode<>(hashA, BigInteger.valueOf(10), DATA);
        tree.getRoots().add(nodeA);

        Hash256 hashB = new Hash256(generateHash(2));
        ForkTree.ForkTreeNode<Integer> nodeB = new ForkTree.ForkTreeNode<>(hashB, BigInteger.valueOf(15), DATA);
        nodeA.getChildren().add(nodeB);

        Hash256 hashD = new Hash256(generateHash(4));
        ForkTree.ForkTreeNode<Integer> nodeD = new ForkTree.ForkTreeNode<>(hashD, BigInteger.valueOf(18), DATA);
        nodeB.getChildren().add(nodeD);

        Hash256 hashC = new Hash256(generateHash(3));
        ForkTree.ForkTreeNode<Integer> nodeC = new ForkTree.ForkTreeNode<>(hashC, BigInteger.valueOf(18), DATA);
        nodeB.getChildren().add(nodeC);

        Hash256 hashE = new Hash256(generateHash(5));
        ForkTree.ForkTreeNode<Integer> nodeE = new ForkTree.ForkTreeNode<>(hashE, BigInteger.valueOf(20), DATA);
        nodeD.getChildren().add(nodeE);

        Hash256 hashF = new Hash256(generateHash(6));
        ForkTree.ForkTreeNode<Integer> nodeF = new ForkTree.ForkTreeNode<>(hashF, BigInteger.valueOf(20), DATA);
        nodeD.getChildren().add(nodeF);

        Hash256 hashNew = new Hash256(generateHash(7));
        ForkTree.ForkTreeNode<Integer> result =
                callFindParentForInsertion(hashNew, BigInteger.valueOf(20), TRUE_BIPREDICATE);

        assertEquals(nodeD, result);
    }

    @Test
    void testFindDeepestAncestor() throws Exception {
        // |A| - B - D
        //  | - C
        Hash256 hashA = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> nodeA = new ForkTree.ForkTreeNode<>(hashA, BigInteger.valueOf(10), DATA);

        Hash256 hashB = new Hash256(generateHash(2));
        ForkTree.ForkTreeNode<Integer> nodeB = new ForkTree.ForkTreeNode<>(hashB, BigInteger.valueOf(15), DATA);

        Hash256 hashC = new Hash256(generateHash(3));
        ForkTree.ForkTreeNode<Integer> nodeC = new ForkTree.ForkTreeNode<>(hashC, BigInteger.valueOf(12), DATA);

        nodeA.getChildren().add(nodeB);
        nodeA.getChildren().add(nodeC);

        Hash256 hashD = new Hash256(generateHash(4));
        ForkTree.ForkTreeNode<Integer> nodeD = new ForkTree.ForkTreeNode<>(hashD, BigInteger.valueOf(18), DATA);

        nodeB.getChildren().add(nodeD);

        ForkTree.ForkTreeNode<Integer> result =
                callFindDeepestAncestor(nodeA, new Hash256(generateHash(5)), BigInteger.valueOf(20), TRUE_BIPREDICATE);

        assertEquals(nodeD, result);
    }

    @Test
    void testFindParentForInsertionPredicateFalse() throws Exception {
        Hash256 hashA = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> nodeA = new ForkTree.ForkTreeNode<>(hashA, BigInteger.valueOf(10), DATA);

        tree.getRoots().add(nodeA);

        Hash256 hashNew = new Hash256(generateHash(2));
        
        ForkTree.ForkTreeNode<Integer> result = 
                callFindParentForInsertion(hashNew, BigInteger.valueOf(20), FALSE_BIPREDICATE);

        assertNull(result);
    }

    @Test
    void testFinalizeNodeRootFound() throws Exception {
        // |A| -> B
        // |C|
        Hash256 hashA = new Hash256(generateHash(1));
        tree.importNode(hashA, BigInteger.valueOf(10), DATA, FALSE_BIPREDICATE);

        Hash256 hashB = new Hash256(generateHash(2));
        tree.importNode(hashB, BigInteger.valueOf(20), 1, TRUE_BIPREDICATE);

        Hash256 hashC = new Hash256(generateHash(3));
        tree.importNode(hashC, BigInteger.valueOf(15), 2, FALSE_BIPREDICATE);

        Optional<Integer> finalizedData = callFinalizeRoot(hashA);
        assertTrue(finalizedData.isPresent());
        assertEquals(DATA, finalizedData.get());

        assertEquals(BigInteger.valueOf(10), tree.getBestFinalizedNumber());

        assertEquals(1, tree.getRoots().size());
        assertEquals(hashB, tree.getRoots().get(0).getHash());
    }

    @Test
    void testFinalizeNodeRootNotFound() throws Exception {
        Hash256 hashA = new Hash256(generateHash(1));
        tree.importNode(hashA, BigInteger.valueOf(30), DATA, FALSE_BIPREDICATE);

        Hash256 nonExistentHash = new Hash256(generateHash(2));
        Optional<Integer> finalizedData = callFinalizeRoot(nonExistentHash);
        assertFalse(finalizedData.isPresent());

        assertEquals(1, tree.getRoots().size());
        assertEquals(hashA, tree.getRoots().get(0).getHash());
    }

    @Test
    void testFinalizeNodeRootAtValidIndex() throws Exception {
        Hash256 hashA = new Hash256(generateHash(1));
        tree.importNode(hashA, BigInteger.valueOf(40), DATA, FALSE_BIPREDICATE);

        Hash256 hashB = new Hash256(generateHash(2));
        tree.importNode(hashB, BigInteger.valueOf(50), 1, TRUE_BIPREDICATE);

        Optional<Integer> finalizedData = callFinalizeRootAt(0);
        assertTrue(finalizedData.isPresent());
        assertEquals(DATA, finalizedData.get());

        assertEquals(BigInteger.valueOf(40), tree.getBestFinalizedNumber());

        assertEquals(1, tree.getRoots().size());
        assertEquals(hashB, tree.getRoots().get(0).getHash());
    }

    @Test
    void testFinalizeNodeRootAtInvalidIndex() throws Exception {
        Hash256 hash = new Hash256(generateHash(1));
        tree.importNode(hash, BigInteger.valueOf(60), DATA, FALSE_BIPREDICATE);

        Optional<Integer> result = callFinalizeRootAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    void testFinalizeNodeCandidateFound() throws Exception {
        Hash256 hashNode = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> node = new ForkTree.ForkTreeNode<>(hashNode, BigInteger.TEN, DATA);

        Hash256 childNodeHash = new Hash256(generateHash(2));
        ForkTree.ForkTreeNode<Integer> childNode = new ForkTree.ForkTreeNode<>(childNodeHash, BigInteger.valueOf(11), DATA);

        node.getChildren().add(childNode);
        tree.getRoots().add(node);

        Optional<Integer> optResult =
                tree.finalizeNode(hashNode, BigInteger.valueOf(10), TRUE_BIPREDICATE, t -> true);

        assertTrue(optResult.isPresent());
        Integer result = optResult.get();

        assertEquals(DATA, result);
        assertEquals(1, tree.getRoots().size());
        assertEquals(childNodeHash, tree.getRoots().get(0).getHash());
        assertEquals(BigInteger.valueOf(10), tree.getBestFinalizedNumber());
    }

    @Test
    void testFinalizeNodeCandidateNotFound() throws Exception {
        Hash256 hashNode = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> node = new ForkTree.ForkTreeNode<>(hashNode, BigInteger.TEN, DATA);
        tree.getRoots().add(node);

        Optional<Integer> optResult =
                tree.finalizeNode(hashNode, BigInteger.valueOf(20), TRUE_BIPREDICATE, t -> false);

        assertTrue(optResult.isEmpty());
        assertEquals(BigInteger.valueOf(20), tree.getBestFinalizedNumber());
    }

    @Test
    void testFinalizeNodeThrowsDueToChildConflict() {
        Hash256 hashCandidate = new Hash256(generateHash(1));
        ForkTree.ForkTreeNode<Integer> candidate = new ForkTree.ForkTreeNode<>(hashCandidate, BigInteger.TEN, DATA);

        Hash256 hashConflictChild = new Hash256(generateHash(2));
        ForkTree.ForkTreeNode<Integer> conflictChild =
                new ForkTree.ForkTreeNode<>(hashConflictChild, BigInteger.valueOf(15), DATA);

        candidate.getChildren().add(conflictChild);

        tree.getRoots().add(candidate);

        assertThrows(ForkTreeException.class, () ->
                tree.finalizeNode(hashCandidate, BigInteger.valueOf(20), TRUE_BIPREDICATE, t -> true)
        );
    }

    @Test
    void testFinalizeNodeLowerNumberThrows() {
        tree.setBestFinalizedNumber(BigInteger.valueOf(30));
        Hash256 hashNode = new Hash256(generateHash(1));

        assertThrows(ForkTreeException.class, () ->
                tree.finalizeNode(hashNode, BigInteger.valueOf(20), TRUE_BIPREDICATE, t -> true)
        );
    }

    @Test
    void testForkTreeNodeGetMaxDepth() throws ForkTreeException {
        // |A| -> B -> D
        // |C|
        Hash256 hashA = new Hash256(generateHash(1));
        tree.importNode(hashA, BigInteger.TEN, DATA, FALSE_BIPREDICATE);

        Hash256 hashB = new Hash256(generateHash(2));
        tree.importNode(hashB, BigInteger.valueOf(20), DATA, TRUE_BIPREDICATE);

        Hash256 hashC = new Hash256(generateHash(3));
        tree.importNode(hashC, BigInteger.valueOf(15), DATA, FALSE_BIPREDICATE);

        Hash256 hashD = new Hash256(generateHash(4));
        tree.importNode(hashD, BigInteger.valueOf(30), DATA, TRUE_BIPREDICATE);

        // Node A depth
        assertEquals(3, tree.getRoots().get(0).getMaxDepth());
        // Node B depth
        assertEquals(2, tree.getRoots().get(0).getChildren().get(0).getMaxDepth());
        // Node D depth
        assertEquals(1, tree.getRoots().get(0).getChildren().get(0).getChildren().get(0).getMaxDepth());
        // Node C depth
        assertEquals(1, tree.getRoots().get(1).getMaxDepth());

    }
    
    private ForkTree.ForkTreeNode<Integer> callFindParentForInsertion(Hash256 hashNew, 
                                                                      BigInteger number, 
                                                                      BiPredicate<Hash256, Hash256> isDescendantOf)
            throws Exception {
        
        Method findParentForInsertionMethod = ForkTree.class.getDeclaredMethod(
                "findParentForInsertion", Hash256.class, BigInteger.class, BiPredicate.class);

        findParentForInsertionMethod.setAccessible(true);

        return (ForkTree.ForkTreeNode<Integer>)
                findParentForInsertionMethod.invoke(tree, hashNew, number, isDescendantOf);
    }

    private ForkTree.ForkTreeNode<Integer> callFindDeepestAncestor(ForkTree.ForkTreeNode<Integer> node,
                                                                   Hash256 hash,
                                                                   BigInteger number,
                                                                   BiPredicate<Hash256, Hash256> isDescendantOf)
            throws Exception {

        Method findDeepestAncestorMethod = ForkTree.class.getDeclaredMethod(
                "findDeepestAncestor",
                ForkTree.ForkTreeNode.class, Hash256.class, BigInteger.class, BiPredicate.class
        );

        findDeepestAncestorMethod.setAccessible(true);

        return (ForkTree.ForkTreeNode<Integer>)
                findDeepestAncestorMethod.invoke(tree, node, hash, number, isDescendantOf);
    }

    private Optional<Integer> callFinalizeRoot(Hash256 hash256) throws Exception{
        Method finalizeRootMethod = ForkTree.class.getDeclaredMethod(
                "finalizeRoot", Hash256.class);

        finalizeRootMethod.setAccessible(true);
        return (Optional<Integer>) finalizeRootMethod.invoke(tree, hash256);
    }

    private Optional<Integer> callFinalizeRootAt(int index) throws Exception {
        Method finalizeRootAtMethod = ForkTree.class.getDeclaredMethod(
                "finalizeRootAt", int.class);

        finalizeRootAtMethod.setAccessible(true);
        return (Optional<Integer>) finalizeRootAtMethod.invoke(tree, index);
    }

    private Iterator<Integer> callIterator() throws Exception {
        Method iteratorMethod = ForkTree.class.getDeclaredMethod(
                "iterator");

        iteratorMethod.setAccessible(true);
        return (Iterator<Integer>) iteratorMethod.invoke(tree);
    }

    private void callRebalance() throws Exception {
        Method rebalanceMethod = ForkTree.class.getDeclaredMethod(
                "rebalance");

        rebalanceMethod.setAccessible(true);
        rebalanceMethod.invoke(tree);
    }

    private byte[] generateHash(int seed) {
        byte[] arr = new byte[32];
        Arrays.fill(arr, (byte) seed);
        return arr;
    }
}
