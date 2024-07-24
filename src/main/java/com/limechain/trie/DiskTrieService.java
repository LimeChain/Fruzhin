package com.limechain.trie;

import com.google.common.primitives.Bytes;
import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.cache.TrieChanges;
import com.limechain.trie.cache.node.PendingInsertUpdate;
import com.limechain.trie.cache.node.PendingRemove;
import com.limechain.trie.cache.node.PendingTrieNodeChange;
import com.limechain.trie.dto.node.DecodedNode;
import com.limechain.trie.dto.node.StorageValue;
import com.limechain.trie.dto.node.TraversedNode;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.TrieNodeData;
import com.limechain.utils.HashUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Log
public class DiskTrieService {

    private final TrieStorage trieStorage;
    public final TrieChanges trieChanges;

    public DiskTrieService(TrieStorage trieStorage) {
        this.trieStorage = trieStorage;

        this.trieChanges = TrieChanges.empty();
    }

    public Optional<byte[]> findStorageValue(byte[] merkleRoot, Nibbles key) {
        Optional<PendingTrieNodeChange> change = trieChanges.getFromCache(key);
        return change.<Optional<byte[]>>map(pendingTrieNodeChange ->
                pendingTrieNodeChange instanceof PendingInsertUpdate update
                    ? Optional.ofNullable(update.value())
                    : Optional.empty())
            .orElseGet(() -> traverseTrie(merkleRoot, key) instanceof TraversalResult.Found found
                ? Optional.ofNullable(found.getFound().getValue())
                : Optional.empty());
    }

    public Optional<Nibbles> getNextKey(byte[] merkleRoot, Nibbles key) {
        PendingInsertUpdate cachedRoot = trieChanges.getRoot().orElse(null);
        TrieNodeData root = cachedRoot != null
            ? new TrieNodeData(cachedRoot.value() != null,
            cachedRoot.partialKey(),
            cachedRoot.childrenMerkleValues(),
            cachedRoot.value(),
            null,
            (byte) cachedRoot.stateVersion().asInt())
            : trieStorage.getTrieNodeFromMerkleValue(merkleRoot);

        if (root == null) {
            return Optional.empty();
        }

        return findNextKey(key, Nibbles.EMPTY, root);
    }

    private Optional<Nibbles> findNextKey(Nibbles prefix, Nibbles currentKey, TrieNodeData node) {
        if (node.getValue() != null && currentKey.compareTo(prefix) > 0) {
            return Optional.of(currentKey);
        }

        for (Nibble nibble : Nibbles.ALL) {
            byte[] childMerkle = node.getChildrenMerkleValues().get(nibble.asInt());
            if (childMerkle == null) {
                continue;
            }

            TrieNodeData cachedChild = getCachedChildAtIndex(currentKey, nibble);
            TrieNodeData childNode = cachedChild != null
                ? cachedChild
                : trieStorage.getTrieNodeFromMerkleValue(childMerkle);

            Nibbles nextPath = currentKey.add(nibble).addAll(childNode.getPartialKey());
            Optional<Nibbles> result = findNextKey(prefix, nextPath, childNode);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    public void upsertNode(byte[] merkleRoot, Nibbles key, byte[] storageValue, StateVersion stateVersion) {
        TreeMap<Nibbles, PendingTrieNodeChange> executionUpdates = new TreeMap<>();

        TraversalResult traversalResult = traverseTrie(merkleRoot, key);
        switch (traversalResult) {
            case TraversalResult.NotFound notFound -> executionUpdates.putAll(
                executeInsert(
                    merkleRoot,
                    key,
                    storageValue,
                    stateVersion,
                    notFound.getClosestAncestor().orElse(null)));
            case TraversalResult.Found found -> {
                TraversedNode foundNode = found.getFound();

                assert key.equals(foundNode.getFullKey())
                    : "Found node in traversal result should have the same key path as the sought key";

                executionUpdates.put(foundNode.getFullKey(), toPendingInsertUpdate(
                    storageValue,
                    foundNode.getPartialKey(),
                    foundNode.getStateVersion(),
                    foundNode.getChildrenMerkleValues(),
                    foundNode.getParent() == null
                ));
            }
            case TraversalResult.Unfinished ignored ->
                throw new IllegalStateException("Traversal result cannot be unfinished at this point in the logic");
        }

        Map.Entry<Nibbles, PendingTrieNodeChange> closestSuccessor = executionUpdates.firstEntry();
        executionUpdates.putAll(toPendingInsertUpdates(traversalResult.traversedNodes,
            closestSuccessor.getKey(),
            closestSuccessor.getValue()));

        trieChanges.updateCache(executionUpdates);
    }

    private TreeMap<Nibbles, PendingInsertUpdate> executeInsert(byte[] merkleRoot,
                                                                Nibbles key,
                                                                byte[] newNodeValue,
                                                                StateVersion newNodeStateVersion,
                                                                @Nullable TraversedNode ancestor) {
        TreeMap<Nibbles, PendingInsertUpdate> result = new TreeMap<>();

        TrieNodeData rootNode = trieStorage.getTrieNodeFromMerkleValue(merkleRoot);
        List<byte[]> newNodeChildren = new ArrayList<>(Collections.nCopies(16, null));

        if (rootNode == null) {
            result.put(key, toPendingInsertUpdate(
                newNodeValue,
                key,
                newNodeStateVersion,
                newNodeChildren,
                true));

            return result;
        }

        int ancestorKeySize = 0;
        byte[] existingNodeMerkle;
        TrieNodeData existingCachedNode = null;

        if (ancestor == null) {
            existingNodeMerkle = merkleRoot;
        } else {
            ancestorKeySize = ancestor.getFullKey().size();
            Nibble newNodeChildIndex = key.get(ancestorKeySize);
            byte[] merkleAtNewNodeIndex = ancestor.getChildrenMerkleValues()
                .get(newNodeChildIndex.asInt());

            if (merkleAtNewNodeIndex == null) {
                result.put(key, toPendingInsertUpdate(
                    newNodeValue,
                    key.drop(ancestorKeySize + 1),
                    newNodeStateVersion,
                    newNodeChildren,
                    false));

                return result;
            } else {
                existingNodeMerkle = merkleAtNewNodeIndex;
                existingCachedNode = getCachedChildAtIndex(ancestor.getFullKey(), newNodeChildIndex);
            }
        }

        TrieNodeData existingNode = existingCachedNode != null
            ? existingCachedNode
            : trieStorage.getTrieNodeFromMerkleValue(existingNodeMerkle);

        Nibbles existingNodePartialKey = existingNode.getPartialKey();

        Nibbles newNodePartialKey = key.drop(ancestorKeySize + 1);

        if (existingNodePartialKey.startsWith(newNodePartialKey)) {
            Nibbles existingNodeFullKey = key.addAll(
                existingNodePartialKey.drop(newNodePartialKey.size()));

            PendingInsertUpdate existing = result.computeIfAbsent(existingNodeFullKey,
                (k) -> toPendingInsertUpdate(
                    existingNode.getValue(),
                    existingNodePartialKey.drop(newNodePartialKey.size() + 1),
                    StateVersion.fromInt(existingNode.getEntriesVersion()),
                    existingNode.getChildrenMerkleValues(),
                    false
                ));

            Nibble existingNodeIndexInNewNode = existingNodePartialKey.get(newNodePartialKey.size());
            newNodeChildren.set(existingNodeIndexInNewNode.asInt(), existing.newMerkleValue());
            result.put(key, toPendingInsertUpdate(
                newNodeValue,
                newNodePartialKey,
                newNodeStateVersion,
                newNodeChildren,
                ancestor == null
            ));

            return result;
        }

        int branchPartialKeyLen = (int) IntStream
            .range(0, Math.min(newNodePartialKey.size(), existingNodePartialKey.size()))
            .takeWhile(i -> newNodePartialKey.get(i).equals(existingNodePartialKey.get(i)))
            .count();

        Nibbles existingNodeFullKey = key.take(ancestorKeySize + 1)
            .addAll(existingNodePartialKey);

        PendingInsertUpdate existingStorage = result.computeIfAbsent(existingNodeFullKey,
            (k) -> toPendingInsertUpdate(
                existingNode.getValue(),
                existingNodePartialKey.drop(branchPartialKeyLen + 1),
                StateVersion.fromInt(existingNode.getEntriesVersion()),
                existingNode.getChildrenMerkleValues(),
                false
            ));

        PendingInsertUpdate newStorage = result.computeIfAbsent(key,
            (k) -> toPendingInsertUpdate(
                newNodeValue,
                newNodePartialKey.drop(branchPartialKeyLen + 1),
                newNodeStateVersion,
                new ArrayList<>(Collections.nCopies(16, null)),
                false
            ));

        Nibble existingNodeIndexInBranchNode = existingNodePartialKey.get(branchPartialKeyLen);
        newNodeChildren.set(existingNodeIndexInBranchNode.asInt(), existingStorage.newMerkleValue());
        Nibble newNodeIndexInBranchNode = newNodePartialKey.get(branchPartialKeyLen);
        newNodeChildren.set(newNodeIndexInBranchNode.asInt(), newStorage.newMerkleValue());

        Nibbles branchNodeFullKey = key.take(ancestorKeySize + 1 + branchPartialKeyLen);

        result.put(branchNodeFullKey, toPendingInsertUpdate(
            null,
            newNodePartialKey.take(branchPartialKeyLen),
            newNodeStateVersion,
            newNodeChildren,
            ancestor == null
        ));

        return result;
    }

    @Nullable
    private TrieNodeData getCachedChildAtIndex(Nibbles parentFullKey, Nibble childIndex) {
        Optional<PendingInsertUpdate> cached = trieChanges.getChildByIndex(parentFullKey, childIndex);

        if (cached.isPresent()) {
            PendingInsertUpdate update = cached.get();
            return new TrieNodeData(
                update.value() == null,
                update.partialKey(),
                new ArrayList<>(update.childrenMerkleValues()),
                update.value(),
                null,
                (byte) update.stateVersion().asInt()
            );
        }

        return null;
    }

    public void deleteStorageNode(byte[] merkleRoot, Nibbles key) {
        TraversalResult traversalResult = traverseTrie(merkleRoot, key);

        switch (traversalResult) {
            case TraversalResult.Found found -> {
                TreeMap<Nibbles, PendingTrieNodeChange> executionUpdates = executeDeletion(found.getFound());
                trieChanges.updateCache(mergeDeletionUpdatesWithTraversed(
                    executionUpdates, traversalResult.traversedNodes));
            }
            case TraversalResult.NotFound ignored -> log.fine("DELETE: Node not found at key " + key);
            case TraversalResult.Unfinished ignored ->
                throw new IllegalStateException("Traversal result cannot be unfinished at this point in the logic");
        }
    }

    public DeleteByPrefixResult deleteMultipleNodesByPrefix(byte[] merkleRoot, Nibbles prefix, Long limit) {
        TraversalResult traversalResult = traverseTrie(merkleRoot, prefix);

        switch (traversalResult) {
            case TraversalResult.Found found -> {
                AtomicInteger deleted = new AtomicInteger(0);

                TraversedNode node = found.getFound();

                for (Nibble nibble : Nibbles.ALL) {
                    byte[] childMerkle = node.getChildrenMerkleValues().get(nibble.asInt());
                    if (childMerkle != null) {
                        Optional<PendingTrieNodeChange> recursionResult = executeRecursiveDeletion(
                            node.getFullKey(), nibble, childMerkle, limit, deleted);
                        if (recursionResult.isPresent()) {
                            PendingTrieNodeChange change = recursionResult.get();
                            node.getChildrenMerkleValues().set(nibble.asInt(),
                                change instanceof PendingInsertUpdate c
                                    ? c.newMerkleValue()
                                    : null);
                        }
                    }
                }

                if (limit != null && deleted.get() >= limit) {
                    return new DeleteByPrefixResult(deleted.get(), false);
                }

                if (node.getValue() != null) {
                    deleted.incrementAndGet();
                }
                TreeMap<Nibbles, PendingTrieNodeChange> executionUpdates = executeDeletion(node);
                trieChanges.updateCache(mergeDeletionUpdatesWithTraversed(
                    executionUpdates, traversalResult.traversedNodes));

                return new DeleteByPrefixResult(deleted.get(), true);
            }
            case TraversalResult.NotFound ignored -> {
                log.fine("DELETE: Node not found at key " + prefix);
                return new DeleteByPrefixResult(0, true);
            }
            case TraversalResult.Unfinished ignored ->
                throw new IllegalStateException("Traversal result cannot be unfinished at this point in the logic");
        }
    }

    private Optional<PendingTrieNodeChange> executeRecursiveDeletion(Nibbles parentFullKey,
                                                                     Nibble indexInParent,
                                                                     byte[] childMerkle,
                                                                     Long limit,
                                                                     AtomicInteger deleted) {
        if (limit != null && deleted.get() >= limit) {
            return Optional.empty();
        }

        TrieNodeData cachedChild = getCachedChildAtIndex(parentFullKey, indexInParent);
        TrieNodeData foundNode = cachedChild != null
            ? cachedChild
            : trieStorage.getTrieNodeFromMerkleValue(childMerkle);
        Nibbles foundNodeFullKey = parentFullKey.add(indexInParent).addAll(foundNode.getPartialKey());
        List<byte[]> foundNodeChildrenCopy = new ArrayList<>(foundNode.getChildrenMerkleValues());

        Optional<PendingTrieNodeChange> recursionResult;
        for (Nibble nibble : Nibbles.ALL) {
            byte[] childMerkleInner = foundNodeChildrenCopy.get(nibble.asInt());
            if (childMerkleInner != null) {
                recursionResult = executeRecursiveDeletion(foundNodeFullKey, nibble, childMerkleInner, limit, deleted);
                if (recursionResult.isPresent()) {
                    PendingTrieNodeChange change = recursionResult.get();
                    foundNodeChildrenCopy.set(nibble.asInt(),
                        change instanceof PendingInsertUpdate c
                            ? c.newMerkleValue()
                            : null);
                }
            }
        }


        if (limit == null || deleted.get() < limit) {
            PendingTrieNodeChange remove = new PendingRemove();
            trieChanges.updateCache(new TreeMap<>(Map.of(foundNodeFullKey, remove)));
            if (foundNode.getValue() != null) {
                deleted.incrementAndGet();
            }
            return Optional.of(remove);
        } else {
            PendingTrieNodeChange update = toPendingInsertUpdate(
                foundNode.getValue(),
                foundNode.getPartialKey(),
                StateVersion.fromInt(foundNode.getEntriesVersion()),
                foundNodeChildrenCopy,
                false
            );
            trieChanges.updateCache(new TreeMap<>(Map.of(foundNodeFullKey, update)));
            return Optional.of(update);
        }
    }

    private TreeMap<Nibbles, PendingTrieNodeChange> executeDeletion(TraversedNode found) {
        TreeMap<Nibbles, PendingTrieNodeChange> result = new TreeMap<>();

        TraversedNode parent = found.getParent();

        if (parent == null) {
            result.put(found.getFullKey(), new PendingRemove());
            return result;
        }

        long foundChildrenCount = found.getChildrenMerkleValues().stream()
            .filter(Objects::nonNull)
            .count();

        if (foundChildrenCount == 0) {
            result.putAll(executeDeletionNoChildren(found, parent));
        } else if (foundChildrenCount == 1) {
            Map.Entry<Nibbles, PendingInsertUpdate> mergedUpdate = mergeParentIntoChild(found, null);
            assert mergedUpdate != null : "Merge result should not be null";
            result.put(mergedUpdate.getKey(), mergedUpdate.getValue());
            result.put(found.getFullKey(), new PendingRemove());
        } else {
            result.put(found.getFullKey(), toPendingInsertUpdate(null,
                found.getPartialKey(),
                found.getStateVersion(),
                found.getChildrenMerkleValues(),
                false));
        }

        return result;
    }

    private TreeMap<Nibbles, PendingTrieNodeChange> executeDeletionNoChildren(TraversedNode found,
                                                                              TraversedNode parent) {
        TreeMap<Nibbles, PendingTrieNodeChange> result = new TreeMap<>();
        result.put(found.getFullKey(), new PendingRemove());

        long parentChildrenCount = parent.getChildrenMerkleValues().stream()
            .filter(Objects::nonNull)
            .count();

        //A leaf node being single child of its parent is invalid scenario.
        assert !(parentChildrenCount == 1 && parent.getValue() == null) : "Unreachable state.";

        if (parentChildrenCount == 2) {
            Nibble foundIndexInParent = found.getFullKey().get(parent.getFullKey().size());
            byte[] foundMerkle = parent.getChildrenMerkleValues().get(foundIndexInParent.asInt());

            Map.Entry<Nibbles, PendingInsertUpdate> mergedUpdate = mergeParentIntoChild(parent, foundMerkle);
            assert mergedUpdate != null : "Merge result should not be null";

            result.put(mergedUpdate.getKey(), mergedUpdate.getValue());
            trieChanges.removeFromCache(parent.getFullKey());

            TraversedNode grandParent = parent.getParent();
            Nibble foundIndexInGrandpa = parent.getFullKey().get(grandParent.getFullKey().size());
            grandParent.getChildrenMerkleValues().set(
                foundIndexInGrandpa.asInt(), mergedUpdate.getValue().newMerkleValue());
            result.put(grandParent.getFullKey(), toPendingInsertUpdate(
                grandParent.getValue(),
                grandParent.getPartialKey(),
                grandParent.getStateVersion(),
                grandParent.getChildrenMerkleValues(),
                grandParent.getParent() == null
            ));
        }

        return result;
    }

    @Nullable
    private Map.Entry<Nibbles, PendingInsertUpdate> mergeParentIntoChild(TraversedNode node,
                                                                         @Nullable byte[] merkleToIgnore) {
        for (int i = 0; i < node.getChildrenMerkleValues().size(); i++) {
            byte[] childMerkle = node.getChildrenMerkleValues().get(i);
            if (childMerkle != null && !Arrays.equals(merkleToIgnore, childMerkle)) {
                Nibble indexInParent = Nibble.fromInt(i);
                TrieNodeData cachedChild = getCachedChildAtIndex(node.getFullKey(), indexInParent);
                TrieNodeData child = cachedChild != null
                    ? cachedChild
                    : trieStorage.getTrieNodeFromMerkleValue(childMerkle);
                Nibbles mergedPartialKey = node.getPartialKey()
                    .add(indexInParent)
                    .addAll(child.getPartialKey());
                Nibbles mergeFullKey = node.getFullKey()
                    .add(indexInParent)
                    .addAll(child.getPartialKey());

                return Map.entry(mergeFullKey, toPendingInsertUpdate(
                    child.getValue(),
                    mergedPartialKey,
                    StateVersion.fromInt(child.getEntriesVersion()),
                    child.getChildrenMerkleValues(),
                    node.getParent() == null
                ));
            }
        }

        return null;
    }

    private TraversalResult traverseTrie(byte[] merkleRoot, Nibbles key) {
        TraversalResult cacheTraverseResult = traverseCache(key);

        if (cacheTraverseResult instanceof TraversalResult.Unfinished tr) {
            TraversalResult diskTraverseResult;
            if (cacheTraverseResult.getTraversedNodes().isEmpty()) {
                diskTraverseResult = traverseDiskFromRoot(merkleRoot, key);
            } else {
                TraversedNode ancestor = cacheTraverseResult.getClosestAncestor().orElse(null);
                assert ancestor != null : "Ancestor cannot be bull at this point.";
                diskTraverseResult = traverseDiskFromAncestor(ancestor, key);
            }

            return tr.finishTraversal(diskTraverseResult);
        }

        return cacheTraverseResult;
    }

    private TraversalResult traverseCache(Nibbles key) {
        List<Map.Entry<Nibbles, PendingInsertUpdate>> entriesInKeyPath =
            trieChanges.getEntriesInKeyPath(PendingInsertUpdate.class, key);

        // If cache is empty or no cached entries are in sought key path continue to db traverse.
        if (trieChanges.isCacheEmpty() || entriesInKeyPath.isEmpty()) {
            return new TraversalResult.Unfinished(new ArrayList<>());
        }

        List<TraversedNode> traversedNodes = toTraversedNodes(entriesInKeyPath);

        // If cache contains exact key match we have 2 options. If it's pending a deletion we return a NotFound result,
        // Found otherwise. No need for db traversal after this.
        if (trieChanges.isKeyInCache(key)) {
            TraversedNode last = traversedNodes.getLast();
            TraversalResult result;

            if (last.getFullKey().equals(key)) {
                traversedNodes.removeLast();
                result = new TraversalResult.Found(traversedNodes, last);
            } else {
                result = new TraversalResult.NotFound(traversedNodes);
            }

            return result;
        }

        // If we reach here we know that there is a closest ancestor in the cache map.
        TraversedNode closestAncestor = traversedNodes.getLast();
        Nibble indexInClosestAncestor = key.get(closestAncestor.getFullKey().size());

        // If the first nibble of the sought key (minus the full key of the closest ancestor) is present in the ancestor
        // this means that there is a following node, so we need to continue traversal in db from that node.
        if (closestAncestor.getChildrenMerkleValues().get(indexInClosestAncestor.asInt()) != null) {
            return new TraversalResult.Unfinished(traversedNodes);
        }

        // Else if no entry is present at the child index no node at the key path exists yet.
        return new TraversalResult.NotFound(traversedNodes);
    }

    private TraversalResult traverseDiskFromAncestor(TraversedNode ancestor, Nibbles key) {
        Nibble childIndexInAncestor = key.get(ancestor.getFullKey().size());
        byte[] childMerkle = ancestor.getChildrenMerkleValues().get(childIndexInAncestor.asInt());
        return traverseDisk(childMerkle, key, ancestor.getFullKey().add(childIndexInAncestor));
    }

    private TraversalResult traverseDiskFromRoot(byte[] merkleRoot, Nibbles key) {
        return traverseDisk(merkleRoot, key, Nibbles.EMPTY);
    }

    private TraversalResult traverseDisk(byte[] merkleRoot,
                                         Nibbles key,
                                         Nibbles ancestorKeyWithChildIndex) {
        TrieNodeData rootNode = trieStorage.getTrieNodeFromMerkleValue(merkleRoot);

        if (rootNode == null) {
            return new TraversalResult.NotFound(new ArrayList<>());
        }

        byte[] currentMerkleValue = merkleRoot;

        List<TraversedNode> traversed = new ArrayList<>();

        Nibbles currentKey = ancestorKeyWithChildIndex;

        Iterator<Nibble> keyIter = key.drop(currentKey.size()).iterator();
        while (true) {
            TrieNodeData currentNode = trieStorage.getTrieNodeFromMerkleValue(currentMerkleValue);

            for (Nibble nibble : currentNode.getPartialKey()) {
                if (!keyIter.hasNext() || !keyIter.next().equals(nibble)) {
                    return new TraversalResult.NotFound(traversed);
                }
            }

            currentKey = currentKey.addAll(currentNode.getPartialKey());

            // TODO 437 add indexinparent to parent field.
            TraversedNode currentTraversed = new TraversedNode(currentKey,
                currentNode.getPartialKey(),
                new ArrayList<>(currentNode.getChildrenMerkleValues()),
                StateVersion.fromInt(currentNode.getEntriesVersion()),
                currentNode.getValue(),
                traversed.isEmpty()
                    ? null
                    : traversed.getLast());

            if (!keyIter.hasNext()) {
                return new TraversalResult.Found(traversed, currentTraversed);
            }

            traversed.add(currentTraversed);

            Nibble childIndex = keyIter.next();
            byte[] nextMerkleValue = currentTraversed.getChildrenMerkleValues().get(childIndex.asInt());

            if (nextMerkleValue == null) {
                return new TraversalResult.NotFound(traversed);
            }

            currentMerkleValue = nextMerkleValue;
            currentKey = currentKey.add(childIndex);
        }
    }

    private static Map<Nibbles, PendingInsertUpdate> toPendingInsertUpdates(List<TraversedNode> toUpdate,
                                                                            Nibbles successorKey,
                                                                            PendingTrieNodeChange successor) {
        Map<Nibbles, PendingInsertUpdate> pendingUpdates = new HashMap<>();

        Nibbles successorFullKey = successorKey.copy();
        byte[] successorMerkleValue = successor instanceof PendingInsertUpdate s
            ? s.newMerkleValue()
            : null;

        for (TraversedNode traversedNode : toUpdate.reversed()) {
            Nibble successorChildIndex = successorFullKey.get(traversedNode.getFullKey().size());
            traversedNode.getChildrenMerkleValues().set(successorChildIndex.asInt(), successorMerkleValue);

            PendingInsertUpdate change = pendingUpdates.compute(traversedNode.getFullKey(), (k, v) ->
                toPendingInsertUpdate(traversedNode.getValue(),
                    traversedNode.getPartialKey(),
                    traversedNode.getStateVersion(),
                    traversedNode.getChildrenMerkleValues(),
                    traversedNode.getParent() == null
                ));

            successorFullKey = traversedNode.getFullKey();
            successorMerkleValue = change.newMerkleValue();
        }

        return pendingUpdates;
    }

    private static PendingInsertUpdate toPendingInsertUpdate(@Nullable byte[] value,
                                                             Nibbles partialKey,
                                                             StateVersion stateVersion,
                                                             List<byte[]> childrenMerkleValues,
                                                             boolean isRoot) {
        List<List<Byte>> childrenMerkles = childrenMerkleValues.stream()
            .map(Optional::ofNullable)
            .map(o -> o
                .map(Bytes::asList)
                .orElse(null))
            .toList();

        DecodedNode<List<Byte>> decoded = new DecodedNode<>(
            childrenMerkles,
            partialKey,
            constructStorageValue(value, stateVersion));

        byte[] merkleValue = decoded.calculateMerkleValue(HashUtils::hashWithBlake2b, isRoot);

        return new PendingInsertUpdate(merkleValue, childrenMerkleValues, partialKey, stateVersion, value);
    }

    private static List<TraversedNode> toTraversedNodes(List<Map.Entry<Nibbles, PendingInsertUpdate>> entries) {
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }

        List<TraversedNode> result = new ArrayList<>();
        TraversedNode parent = null;

        for (Map.Entry<Nibbles, PendingInsertUpdate> e : entries) {
            PendingInsertUpdate currentPendingChange = e.getValue();

            TraversedNode newTraversalNode = new TraversedNode(e.getKey(),
                currentPendingChange.partialKey(),
                new ArrayList<>(currentPendingChange.childrenMerkleValues()),
                currentPendingChange.stateVersion(),
                currentPendingChange.value(),
                parent);

            parent = newTraversalNode;
            result.add(newTraversalNode);
        }

        return result;
    }

    @Nullable
    private static StorageValue constructStorageValue(@Nullable byte[] value, StateVersion stateVersion) {
        if (value == null) {
            return null;
        }

        if (stateVersion == StateVersion.V1 && value.length >= 33) {
            return new StorageValue(HashUtils.hashWithBlake2b(value), true);
        }

        return new StorageValue(value, false);
    }

    private static TreeMap<Nibbles, PendingTrieNodeChange> mergeDeletionUpdatesWithTraversed(
        TreeMap<Nibbles, PendingTrieNodeChange> pendingUpdates,
        List<TraversedNode> traversedNodes) {
        TreeMap<Nibbles, PendingTrieNodeChange> result = new TreeMap<>(pendingUpdates);

        Map.Entry<Nibbles, PendingTrieNodeChange> closestSuccessor = result.size() != 1
            ? result.entrySet().stream()
            .filter(e -> e.getValue() instanceof PendingInsertUpdate)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("There should be at least one pending update."))
            : result.firstEntry();

        List<TraversedNode> toUpdate = traversedNodes;
        if (result.size() == 3) {
            toUpdate = toUpdate.subList(0, toUpdate.size() - 2);
        }

        result.putAll(toPendingInsertUpdates(toUpdate,
            closestSuccessor.getKey(),
            closestSuccessor.getValue()));

        return result;
    }

    /**
     * Represents the outcome of a trie traversal operation.
     * Because we check our cache layer first, see {@link TrieChanges}{@code .changes},
     * there is a chance that the traversal will result in an {@link Unfinished} result.
     * This means that the traversal has to continue in the database stored nodes.
     */
    @Getter
    private sealed abstract static class TraversalResult {

        /**
         * All the nodes visited during the traversal. Could be empty in case of an empty trie or when the sought key
         * is lexicographically before the current root.
         */
        private final List<TraversedNode> traversedNodes;

        private TraversalResult(List<TraversedNode> traversedNodes) {
            this.traversedNodes = traversedNodes;
        }

        /**
         * In the case of a non-empty traversal list the last entry will always be the closest ancestor of the result.
         *
         * @return The closest ancestor node at this point of the traversal.
         */
        Optional<TraversedNode> getClosestAncestor() {
            if (traversedNodes.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(traversedNodes.getLast());
        }

        /**
         * Traversal is finished and a node at the sought key path was found.
         */
        @Getter
        private static final class Found extends DiskTrieService.TraversalResult {
            private final TraversedNode found;

            private Found(List<TraversedNode> traversedNodes, TraversedNode found) {
                super(traversedNodes);

                this.found = found;
            }
        }

        /**
         * Traversal is finished and no node at the sought key path was found.
         */
        private static final class NotFound extends DiskTrieService.TraversalResult {
            private NotFound(List<TraversedNode> traversedNodes) {
                super(traversedNodes);
            }
        }

        /**
         * Traversal has gone through the cache layer, but has not found the appropriate node at a given key path.
         */
        private static final class Unfinished extends DiskTrieService.TraversalResult {
            private Unfinished(List<TraversedNode> traversedNodes) {
                super(traversedNodes);
            }

            /**
             * Concatenates the results of the unfinished traversal with a finished one.
             *
             * @param finishedTraversal a finished traversal with either a found or a non found result.
             * @return a finished traversal with a concatenated list of nodes from both traversals.
             */
            TraversalResult finishTraversal(TraversalResult finishedTraversal) {
                if (!getTraversedNodes().isEmpty()) {
                    TraversedNode parent = getTraversedNodes().getLast();
                    switch (finishedTraversal) {
                        case Found found -> {
                            TraversedNode child = found.getTraversedNodes().isEmpty()
                                ? found.getFound()
                                : found.getTraversedNodes().getFirst();
                            child.setParent(parent);
                        }
                        case NotFound notFound -> {
                            if (!notFound.getTraversedNodes().isEmpty()) {
                                TraversedNode child = notFound.getTraversedNodes().getFirst();
                                child.setParent(parent);
                            }
                        }
                        case Unfinished ignored ->
                            throw new IllegalStateException("Cannot finish a traversal with another unfinished one.");
                    }
                }

                getTraversedNodes().addAll(finishedTraversal.getTraversedNodes());

                return finishedTraversal instanceof Found f
                    ? new Found(getTraversedNodes(), f.getFound())
                    : new NotFound(getTraversedNodes());
            }
        }
    }
}
