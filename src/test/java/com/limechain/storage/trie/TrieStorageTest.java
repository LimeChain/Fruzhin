package com.limechain.storage.trie;


import com.limechain.storage.KVRepository;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.TrieNodeData;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrieStorageTest {
    private static final String ROOT_HASH = "0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f347";

    @Mock
    private KVRepository<String, Object> db;

    @InjectMocks
    private TrieStorage trieStorage;

    @Test
    void testGetByKeyFromBlock() {
        Nibbles key = Nibbles.fromBytes("testKey".getBytes());
        byte[] expectedValue = "testValue".getBytes();

        final TrieNodeData trieNodeData =
            new TrieNodeData(
                false,
                key,
                IntStream.range(0, 16).mapToObj(__ -> (byte[]) null).toList(),
                expectedValue,
                new byte[0],
                (byte) 0
            );

        when(db.find(anyString())).thenReturn(Optional.of(trieNodeData));

        byte[] blockStateRoot = Hash256.from(ROOT_HASH).getBytes();
        Optional<NodeData> result = trieStorage.getByKeyFromMerkle(blockStateRoot, key);

        assertTrue(result.isPresent());
        assertArrayEquals(expectedValue, result.get().getValue());

        verify(db).find(anyString());
    }

    @Test
    void testGetByKeyFromBlockWithNonMatchingKey() {
        String keyStr = "nonMatchingKey";

        when(db.find(anyString())).thenReturn(Optional.empty());

        byte[] blockStateRoot = Hash256.from(ROOT_HASH).getBytes();
        Optional<NodeData> result =
            trieStorage.getByKeyFromMerkle(blockStateRoot, Nibbles.fromBytes(keyStr.getBytes()));

        assertTrue(result.isEmpty());

        verify(db).find(anyString());
    }

    @Test
    void testGetByKeyFromBlockWhenTrieNodeDoesNotMatch() {
        Nibbles key = Nibbles.fromBytes("testKey".getBytes());

        // Simulate a trie node that does not directly match the provided key
        TrieNodeData nonMatchingTrieNodeData = new TrieNodeData(
            false,
            Nibbles.fromHexString("01"), // Partial key that does not match the test key
            IntStream.range(0, 16).mapToObj(__ -> (byte[]) null).toList(),
            null,
            new byte[0],
            (byte) 0);
        when(db.find(anyString())).thenReturn(Optional.of(nonMatchingTrieNodeData));

        // Action
        byte[] blockStateRoot = Hash256.from(ROOT_HASH).getBytes();
        Optional<NodeData> result = trieStorage.getByKeyFromMerkle(blockStateRoot, key);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetNextKey() {
        Nibbles prefix = Nibbles.fromBytes("nextKe".getBytes());
        Nibbles actualKey = Nibbles.fromBytes("nextKey".getBytes());

        // Setup a mock TrieNodeData that represents the next key
        TrieNodeData nextKeyNode = new TrieNodeData(
            false,
            actualKey,
            new ArrayList<>(), "nextValue".getBytes(), new byte[0], (byte) 0);

        // Assuming the database returns the mock TrieNodeData for the next key
        when(db.find(anyString())).thenReturn(Optional.of(nextKeyNode));

        // Action
        byte[] blockStateRoot = Hash256.from(ROOT_HASH).getBytes();
        Nibbles result = trieStorage.getNextKeyByMerkleValue(blockStateRoot, prefix);

        // Assert
        assertEquals(actualKey, result);
    }
}