package com.limechain.trie;

import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.dto.node.StorageNode;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class BlockTrieAccessorTest {
    @Spy
    private TrieStructure<NodeData> partialTrie;

    private TrieStructure<NodeData> fullTrie;

    @Mock
    private TrieStorage trieStorage;

    @Mock
    private Hash256 blockHash;

    @InjectMocks
    private BlockTrieAccessor blockTrieAccessor;

    private final List<StorageNode> initStorageNodes = List.of(
            new StorageNode(Nibbles.fromHexString("a1b"), new NodeData(new byte[] { 1, 2, 3 })),
            new StorageNode(Nibbles.fromHexString("a813f"), new NodeData(new byte[] { 1, 4, 8 })),
            new StorageNode(Nibbles.fromHexString("ab2"), new NodeData(new byte[] { 6, 2, 5 })),
            new StorageNode(Nibbles.fromHexString("a81"), new NodeData(new byte[] { 6, 2, 5 }))
    );

    private final StorageNode lonelyChild =
            new StorageNode(Nibbles.fromHexString("a1b1111"), new NodeData(new byte[]{ 12, 13, 14}));

    @BeforeEach
    void setup() {
        fullTrie = new TrieStructure<>();
        initStorageNodes.forEach(node -> fullTrie.insertNode(node.key(), node.nodeData()));
        fullTrie.insertNode(lonelyChild.key(), lonelyChild.nodeData());
        TrieStructureFactory.calculateMerkleValues(fullTrie, StateVersion.V0, HashUtils::hashWithBlake2b);
        fullTrie.streamOrdered()
                .map(index -> fullTrie.nodeHandleAtIndex(index))
                .filter(NodeHandle::hasStorageValue)
                .forEach(nodeHandle -> {
                    Nibbles key = nodeHandle.getFullKey();
                    NodeData userData = nodeHandle.getUserData();
                    if (userData == null) {
                        return;
                    }
                    partialTrie.insertNode(key, new NodeData(
                            key.equals(lonelyChild.key()) ? null : userData.getValue(),
                            userData.getMerkleValue()
                    ));
                });
    }


    @Test
    void find() {
        StorageNode storageNode = initStorageNodes.get(1);
        byte[] result = blockTrieAccessor.find(storageNode.key()).orElse(null);

        assertEquals(storageNode.nodeData().getValue(), result);
    }

    @Test
    void save() {
        Nibbles key = Nibbles.fromHexString("abcde");
        byte[] value = new byte[] { 17, 1, 62};
        blockTrieAccessor.save(key, value);

        byte[] result = Objects.requireNonNull(partialTrie.node(key).asNodeHandle().getUserData()).getValue();
        assertArrayEquals(value, result);
    }

    @Test
    void merkle() {
        byte[] result = blockTrieAccessor.getMerkleRoot(StateVersion.V0);
        assertArrayEquals(fullTrieMerkleRoot() ,result);
    }

    @Test
    void merkleRootAfterAddLeafNode() {
        addNode(Nibbles.fromHexString("ab2aa"), new byte[] { 6, 2, 5 });

        byte[] result = blockTrieAccessor.getMerkleRoot(StateVersion.V0);

        assertArrayEquals(fullTrieMerkleRoot() ,result);
    }

    @Test
    void merkleRootAfterAddBranchNode() {
        addNode(Nibbles.fromHexString("a1"), new byte[] { 2, 5, 9, 1 });
        addNode(Nibbles.fromHexString("a"), new byte[] { 72, 5, 11, 1 });

        byte[] result = blockTrieAccessor.getMerkleRoot(StateVersion.V0);

        assertArrayEquals(fullTrieMerkleRoot() ,result);
    }

    private void addNode(Nibbles key, byte[] value) {
        fullTrie.insertNode(key, new NodeData(value));
        blockTrieAccessor.save(key, value);
    }

    @Test
    void merkleRootAfterDelete() {
        removeNode(Nibbles.fromHexString("a81"));

        byte[] result = blockTrieAccessor.getMerkleRoot(StateVersion.V0);

        assertArrayEquals(fullTrieMerkleRoot() ,result);
    }

    private void removeNode(Nibbles key) {
        fullTrie.removeValueAtKey(key);
        blockTrieAccessor.delete(key);
    }

    private byte[] fullTrieMerkleRoot() {
        TrieStructureFactory.calculateMerkleValues(fullTrie, StateVersion.V0, HashUtils::hashWithBlake2b);
        return fullTrie.getRootNode()
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElse(null);
    }
}