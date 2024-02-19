package com.limechain.storage.trie;


import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.BlockState;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.TrieNodeData;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Optional;

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
        String keyStr = "testKey";
        byte[] expectedValue = "testValue".getBytes();
        Hash256 mockBlockHash = Hash256.from(BLOCK_HASH);
        BlockHeader mockBlockHeader = mock(BlockHeader.class);
        when(mockBlockHeader.getStateRoot()).thenReturn(Hash256.from(ROOT_HASH));

        final TrieNodeData trieNodeData =
                new TrieNodeData(
                        trieStorage.partialKeyFromNibbles(Nibbles.fromBytes(keyStr.getBytes()).asUnmodifiableList()),
                        new ArrayList<>(), expectedValue, new byte[0], (byte) 0);


        when(db.find(anyString())).thenReturn(Optional.of(trieNodeData));
        when(blockState.getHeader(mockBlockHash)).thenReturn(mockBlockHeader);

        Optional<byte[]> result = trieStorage.getByKeyFromBlock(mockBlockHash, keyStr);

        assertTrue(result.isPresent());
        assertArrayEquals(expectedValue, result.get());

        verify(db).find(anyString());
    }

    @Test
    void testGetByKeyFromBlockWithNonMatchingKey() {
        String keyStr = "nonMatchingKey";
        Hash256 mockBlockHash = Hash256.from(BLOCK_HASH);
        BlockHeader mockBlockHeader = mock(BlockHeader.class);
        when(mockBlockHeader.getStateRoot()).thenReturn(Hash256.from(ROOT_HASH));
        when(blockState.getHeader(mockBlockHash)).thenReturn(mockBlockHeader);

        when(db.find(anyString())).thenReturn(Optional.empty());

        Optional<byte[]> result = trieStorage.getByKeyFromBlock(mockBlockHash, keyStr);

        assertTrue(result.isEmpty());

        verify(db).find(anyString());
    }

    @Test
    void testGetByKeyFromBlockWithNullBlockHeader() {
        String keyStr = "testKey";
        Hash256 mockBlockHash = Hash256.from(BLOCK_HASH);

        when(blockState.getHeader(mockBlockHash)).thenReturn(null);

        Optional<byte[]> result = trieStorage.getByKeyFromBlock(mockBlockHash, keyStr);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetByKeyFromBlockWhenTrieNodeDoesNotMatch() {
        String keyStr = "testKey";
        Hash256 mockBlockHash = Hash256.from(BLOCK_HASH);
        BlockHeader mockBlockHeader = mock(BlockHeader.class);
        when(mockBlockHeader.getStateRoot()).thenReturn(Hash256.from(ROOT_HASH));

        // Simulate a trie node that does not directly match the provided key
        TrieNodeData nonMatchingTrieNodeData = new TrieNodeData(
                new byte[]{0x01}, // Partial key that does not match the test key
                new ArrayList<>(), null, new byte[0], (byte) 0);
        when(db.find(anyString())).thenReturn(Optional.of(nonMatchingTrieNodeData));
        when(blockState.getHeader(mockBlockHash)).thenReturn(mockBlockHeader);

        // Action
        Optional<byte[]> result = trieStorage.getByKeyFromBlock(mockBlockHash, keyStr);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetNextKey() {
        String prefixStr = "nextKe";
        Hash256 blockHash = Hash256.from(BLOCK_HASH);
        BlockHeader mockBlockHeader = mock(BlockHeader.class);

        when(mockBlockHeader.getStateRoot()).thenReturn(Hash256.from(ROOT_HASH));
        when(blockState.getHeader(blockHash)).thenReturn(mockBlockHeader);

        // Setup a mock TrieNodeData that represents the next key
        TrieNodeData nextKeyNode = new TrieNodeData(
                trieStorage.partialKeyFromNibbles(Nibbles.fromBytes("nextKey".getBytes()).asUnmodifiableList()),
                new ArrayList<>(), "nextValue".getBytes(), new byte[0], (byte) 0);

        // Assuming the database returns the mock TrieNodeData for the next key
        when(db.find(anyString())).thenReturn(Optional.of(nextKeyNode));

        // Action
        String result = trieStorage.getNextKey(blockHash, prefixStr);

        // Assert
        assertEquals("nextKey", result);
    }
}