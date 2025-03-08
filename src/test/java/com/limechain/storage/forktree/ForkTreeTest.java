package com.limechain.storage.forktree;

import com.limechain.exception.forktree.DuplicateException;
import com.limechain.exception.forktree.RevertException;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForkTreeTest {

    private static final BiPredicate<Hash256, Hash256> FALSE_BIPREDICATE = (a, b) -> false;
    private static final BiPredicate<Hash256, Hash256> TRUE_BIPREDICATE = (a, b) -> true;
    private static final Integer DATA = 42;

    @Test
    void testImportNodeAsRoot() throws Exception {
        ForkTree<Integer> tree = new ForkTree<>();
        Hash256 hash = new Hash256(generateHash(1));
        boolean isRoot = tree.importNode(hash, BigInteger.ONE, DATA, FALSE_BIPREDICATE);

        assertTrue(isRoot);
        assertEquals(1, tree.getRoots().size());
        assertEquals(hash, tree.getRoots().get(0).getHash());
    }

    @Test
    void testImportRootNodeAndChildNode() throws Exception {
        ForkTree<Integer> tree = new ForkTree<>();

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
        ForkTree<Integer> tree = new ForkTree<>();
        tree.setBestFinalizedNumber(Optional.of(BigInteger.TEN));

        Hash256 hashA = new Hash256(generateHash(1));

        assertThrows(RevertException.class,
                () -> tree.importNode(hashA, BigInteger.ONE, 1, FALSE_BIPREDICATE));
    }

    @Test
    void testImportNodeDuplicateInRoots() throws Exception {
        ForkTree<Integer> tree = new ForkTree<>();
        Hash256 hash = new Hash256(generateHash(1));
        tree.importNode(hash, BigInteger.ONE, DATA, FALSE_BIPREDICATE);

        assertThrows(DuplicateException.class, () ->
                tree.importNode(hash, BigInteger.valueOf(1), DATA, FALSE_BIPREDICATE));
    }

    @Test
    void testImportNodeDuplicateInChildren() throws Exception {
        ForkTree<Integer> tree = new ForkTree<>();
        Hash256 hashRoot = new Hash256(generateHash(1));

        tree.importNode(hashRoot, BigInteger.ONE, DATA, FALSE_BIPREDICATE);

        Hash256 hashChild = new Hash256(generateHash(2));
        tree.importNode(hashChild, BigInteger.TWO, DATA, TRUE_BIPREDICATE);

        assertThrows(DuplicateException.class, () ->
                tree.importNode(hashChild, BigInteger.TWO, DATA, TRUE_BIPREDICATE));
    }

    @Test
    void testImportNodeMultiplePotentialParents() throws Exception {
        ForkTree<Integer> tree = new ForkTree<>();

        // |A| -> B -> C
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

//
//    /**
//     * Test that importing a duplicate node (same hash) throws DuplicateException.
//     */
//    @Test(expected = DuplicateException.class)
//    public void testImportNodeDuplicate() throws Exception {
//        ForkTree<Integer> tree = new ForkTree<>();
//        Hash256 hashA = new Hash256("A");
//        tree.importNode(hashA, BigInteger.TEN, 1, isDescendantOf);
//        // Importing the same node again should throw.
//        tree.importNode(hashA, BigInteger.TEN, 1, isDescendantOf);
//    }
//
//    /**
//     * Test that finalizeWithDescendentIf properly finalizes a candidate root.
//     * In this test, we import node A as a root with a child B.
//     * Then we finalize A with number 10. The expected behavior is that A is removed,
//     * its children (B) become the new roots, and bestFinalizedNumber is updated.
//     */
//    @Test
//    public void testFinalizeWithDescendentIf() throws Exception {
//        ForkTree<Integer> tree = new ForkTree<>();
//        Hash256 hashA = new Hash256("A");
//        Hash256 hashB = new Hash256("B");
//
//        // Import node A as root with number 10 and data 1.
//        tree.importNode(hashA, BigInteger.TEN, 1, isDescendantOf);
//        // Import node B as child of A with number 20 and data 2.
//        tree.importNode(hashB, BigInteger.valueOf(20), 2, isDescendantOf);
//
//        // Use a predicate that approves data equal to 1.
//        Predicate<Integer> predicate = data -> data == 1;
//
//        // Finalize with hash "A" and number 10.
//        tree.finalizeWithDescendentIf(hashA, BigInteger.TEN, isDescendantOf, predicate);
//
//        // After finalization:
//        // bestFinalizedNumber should be 10,
//        // and the tree's roots should now be A's children (i.e. node B).
//        Assert.assertEquals("Best finalized number should be 10", Optional.of(BigInteger.TEN), tree.getBestFinalizedNumber());
//        Assert.assertEquals("There should be 1 root after finalization", 1, tree.getRoots().size());
//        Assert.assertEquals("The remaining root should be B", hashB, tree.getRoots().get(0).hash);
//    }
//
//    @Test(expected = UnfinalizedAncestor.class)
//    public void testFinalizeWithDescendentIfUnfinalizedChild() throws Exception {
//        ForkTree<Integer> tree = new ForkTree<>();
//        Hash256 hashA = new Hash256("A");
//        Hash256 hashB = new Hash256("B");
//
//        // Import node A as root with number 10 and data 1.
//        tree.importNode(hashA, BigInteger.TEN, 1, isDescendantOf);
//        // Import node B as a child of A with number 5 (which is <= 10) and data 2.
//        tree.importNode(hashB, BigInteger.valueOf(5), 2, isDescendantOf);
//
//        // Finalizing A with number 10 should detect that B (with number 5) is unfinalized.
//        tree.finalizeWithDescendentIf(hashA, BigInteger.TEN, isDescendantOf, data -> true);
//    }

    @Test
    void testIteratorBFS() throws Exception {
        ForkTree<Integer> tree = new ForkTree<>();

        Hash256 hashA = new Hash256(generateHash(1));
        Hash256 hashB = new Hash256(generateHash(2));
        Hash256 hashC = new Hash256(generateHash(3));

        // |A|
        // |B|
        // |C|
        tree.importNode(hashA, BigInteger.TEN, 1, FALSE_BIPREDICATE);
        tree.importNode(hashB, BigInteger.valueOf(20), 2, FALSE_BIPREDICATE);
        tree.importNode(hashC, BigInteger.valueOf(30), 3, FALSE_BIPREDICATE);

        Iterator<Integer> it = tree.iterator();

        List<Integer> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        assertArrayEquals(new Integer[]{1, 2, 3}, result.toArray(new Integer[0]));
    }

    @Test
    void testIteratorWithForkTreeHavingBranches() throws Exception {
        ForkTree<Integer> tree = new ForkTree<>();

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

        Iterator<Integer> it = tree.iterator();
        List<Integer> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        assertArrayEquals(new Integer[]{1, 3, 2, 4, 5}, result.toArray(new Integer[0]));
    }

    private byte[] generateHash(int seed) {
        byte[] arr = new byte[32];
        Arrays.fill(arr, (byte) seed);
        return arr;
    }
}
