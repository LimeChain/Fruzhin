package com.limechain.storage.trie;


import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.BlockState;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.TrieNodeData;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrieStorageTest {

    private static final String BLOCK_HASH = "0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f346";
    private static final String ROOT_HASH = "0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f347";
    private final TrieStorage trieStorage = TrieStorage.getInstance();
    private final BlockState blockState = mock(BlockState.class);
    @Mock
    private KVRepository<String, Object> db;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        trieStorage.initialize(db, blockState);
    }

    @Test
    void testGetByKeyFromBlock() {
        Nibbles key = Nibbles.fromBytes("testKey".getBytes());
        byte[] expectedValue = "testValue".getBytes();
//        Hash256 mockBlockHash = Hash256.from(BLOCK_HASH);
//        BlockHeader mockBlockHeader = mock(BlockHeader.class);
//        when(mockBlockHeader.getStateRoot()).thenReturn(Hash256.from(ROOT_HASH));

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
//        when(blockState.getHeader(mockBlockHash)).thenReturn(mockBlockHeader);
//        when(blockState.getBlockStateRoot(mockBlockHash)).thenReturn(mockBlockHash);

        byte[] blockStateRoot = Hash256.from(ROOT_HASH).getBytes();
        Optional<NodeData> result = trieStorage.getByKeyFromMerkle(blockStateRoot, key);

        assertTrue(result.isPresent());
        assertArrayEquals(expectedValue, result.get().getValue());

        verify(db).find(anyString());
    }

    @Test
    void testGetByKeyFromBlockWithNonMatchingKey() {
        String keyStr = "nonMatchingKey";
//        Hash256 mockBlockHash = Hash256.from(BLOCK_HASH);
//        BlockHeader mockBlockHeader = mock(BlockHeader.class);
//        when(mockBlockHeader.getStateRoot()).thenReturn(Hash256.from(ROOT_HASH));
//        when(blockState.getHeader(mockBlockHash)).thenReturn(mockBlockHeader);

        when(db.find(anyString())).thenReturn(Optional.empty());

        byte[] blockStateRoot = Hash256.from(ROOT_HASH).getBytes();
        Optional<NodeData> result = trieStorage.getByKeyFromMerkle(blockStateRoot, Nibbles.fromBytes(keyStr.getBytes()));

        assertTrue(result.isEmpty());

        verify(db).find(anyString());
    }

//    @Test
//    void testGetByKeyFromBlockWithNullBlockHeader() {
//        Nibbles key = Nibbles.fromBytes("testKey".getBytes());
////        Hash256 mockBlockHash = Hash256.from(BLOCK_HASH);
////
////        when(blockState.getHeader(mockBlockHash)).thenReturn(null);
//
//        byte[] blockStateRoot = Hash256.from(ROOT_HASH).getBytes();
//        Optional<NodeData> result = trieStorage.getByKeyFromMerkle(blockStateRoot, key);
//
//        assertTrue(result.isEmpty());
//    }

    @Test
    void testGetByKeyFromBlockWhenTrieNodeDoesNotMatch() {
        Nibbles key = Nibbles.fromBytes("testKey".getBytes());
//        Hash256 mockBlockHash = Hash256.from(BLOCK_HASH);
//        BlockHeader mockBlockHeader = mock(BlockHeader.class);
//        when(mockBlockHeader.getStateRoot()).thenReturn(Hash256.from(ROOT_HASH));

        // Simulate a trie node that does not directly match the provided key
        TrieNodeData nonMatchingTrieNodeData = new TrieNodeData(
                false,
                Nibbles.fromHexString("01"), // Partial key that does not match the test key
                IntStream.range(0, 16).mapToObj(__ -> (byte[]) null).toList(),
                null,
                new byte[0],
                (byte) 0);
        when(db.find(anyString())).thenReturn(Optional.of(nonMatchingTrieNodeData));
//        when(blockState.getHeader(mockBlockHash)).thenReturn(mockBlockHeader);

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
//        Hash256 blockHash = Hash256.from(BLOCK_HASH);
//        BlockHeader mockBlockHeader = mock(BlockHeader.class);
//
//        when(mockBlockHeader.getStateRoot()).thenReturn(Hash256.from(ROOT_HASH));
//        when(blockState.getHeader(blockHash)).thenReturn(mockBlockHeader);

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