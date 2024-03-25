package com.limechain.runtime.hostapi;

import com.limechain.runtime.Runtime;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.trie.TrieStructureFactory;
import com.limechain.trie.decoded.Trie;
import com.limechain.trie.decoded.TrieVerifier;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.utils.RandomGenerationUtils;
import com.limechain.utils.scale.ScaleUtils;
import org.javatuples.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrieHostFunctionsTest {
    final static int TRIE_ROOT_HASH_BYTE_LEN = 32;
    private final byte[] keyBytes = new byte[] {1, 2, 3};
    private final byte[] valueBytes = new byte[] {4, 5, 6};
    @InjectMocks
    private TrieHostFunctions trieHostFunctions;
    @Mock
    private Runtime runtime;
    @Mock
    private RuntimePointerSize keyPointer;
    @Mock
    private RuntimePointerSize valuePointer;
    @Mock
    private RuntimePointerSize expectedResultPtr;
    @Mock
    private Trie trie;

    @Test
    void blakeTwo256OrderedRootVersion1() throws IOException {
        var expectedRootHash = RandomGenerationUtils.generateBytes(TRIE_ROOT_HASH_BYTE_LEN);
        var values = Stream.of("first", "second", "third").map(String::getBytes).toList();
        when(runtime.writeDataToMemory(eq(expectedRootHash))).thenReturn(expectedResultPtr);

        when(runtime.writeDataToMemory(expectedRootHash)).thenReturn(expectedResultPtr);
        try (
            MockedConstruction<TrieHostFunctions.ArgParser> argParserMockedConstruction = mockConstruction(
                TrieHostFunctions.ArgParser.class,
                (mock, context) -> {
                    when(mock.parseOrderedValues(anyInt())).thenReturn(values);
                });
            MockedConstruction<TrieHostFunctions.TrieRootCalculator> trieRootCalculatorMockedConstruction = mockConstruction(
                TrieHostFunctions.TrieRootCalculator.class,
                (mock, context) -> {
                    when(mock.orderedTrieRoot(eq(values))).thenReturn(expectedRootHash);
                })
        ) {
            // Call the actual function
            int resultPtr = (int) trieHostFunctions.ext_trie_blake2_256_ordered_root_version_1(null);

            // Assert we got the expected pointer
            assertEquals(expectedResultPtr.pointer(), resultPtr);

            // Assert only one parser was created
            var mockedArgsParsers = argParserMockedConstruction.constructed();
            assertEquals(1, mockedArgsParsers.size());

            // And it had to parse KVPs at position 0
            var mockedArgsParser = mockedArgsParsers.get(0);
            verify(mockedArgsParser).parseOrderedValues(anyInt());

            // Verify proper trie root calculation method was called
            verify(trieRootCalculatorMockedConstruction.constructed().get(0)).orderedTrieRoot(eq(values));
        }
    }

    @Test
    @Disabled("Java can't mock sealed classes... I think. Issue with mocking `NodeHandle`.")
    void Test_blakeTwo256RootVersion1_Outermocks() {
        var trieMock = mock(TrieStructure.class);
        var nodeHandleMock = mock(NodeHandle.class);
        byte[] expectedRootHash = mock(byte[].class);
        when(nodeHandleMock.getUserData()).thenReturn(new NodeData(new byte[] {}, expectedRootHash));
        when(trieMock.getRootNode()).thenReturn(Optional.of(nodeHandleMock));

        when(runtime.writeDataToMemory(expectedRootHash)).thenReturn(expectedResultPtr);

        try (MockedStatic<TrieStructureFactory> mockedTSF = Mockito.mockStatic(TrieStructureFactory.class)) {
            mockedTSF
                .when(() -> TrieStructureFactory.buildTrieStructure(any()))
                .thenReturn(trieMock);

            // Call the actual function
            int resultPtr = (int) trieHostFunctions.ext_trie_blake2_256_root_version_1(null);

            // Assert we got the expected pointer
            assertEquals(expectedResultPtr.pointer(), resultPtr);
        }
    }

    @Test
    void Test_blakeTwo256RootVersion1() {
        var expectedRootHash = RandomGenerationUtils.generateBytes(TRIE_ROOT_HASH_BYTE_LEN);
        var pairs = Stream.of(new Pair<>("first", "one"), new Pair<>("second", "two"))
            .map(p -> new Pair<>(p.getValue0().getBytes(), p.getValue1().getBytes()))
            .toList();
        when(runtime.writeDataToMemory(eq(expectedRootHash))).thenReturn(expectedResultPtr);

        try (
            MockedConstruction<TrieHostFunctions.ArgParser> argParserMockedConstruction = mockConstruction(
                TrieHostFunctions.ArgParser.class,
                (mock, context) -> {
                    when(mock.parseKeyValuePairs(anyInt())).thenReturn(pairs);
                });
            MockedConstruction<TrieHostFunctions.TrieRootCalculator> ignored2 = mockConstruction(
                TrieHostFunctions.TrieRootCalculator.class,
                (mock, context) -> {
                    when(mock.trieRoot(eq(pairs))).thenReturn(expectedRootHash);
                })
        ) {
            // Call the actual function
            int resultPtr = (int) trieHostFunctions.ext_trie_blake2_256_root_version_1(null);

            // Assert we got the expected pointer
            assertEquals(expectedResultPtr.pointer(), resultPtr);

            // Assert only one parser was created
            var mockedArgsParsers = argParserMockedConstruction.constructed();
            assertEquals(1, mockedArgsParsers.size());

            // And it had to parse KVPs at position 0
            var mockedArgsParser = mockedArgsParsers.get(0);
            verify(mockedArgsParser).parseKeyValuePairs(anyInt());
        }
    }

    @Test
    @Disabled("Mockito can't properly manage something being equal about both key and value mocks. " +
              "Or perhaps it has to do with my method invocations on them.")
    void blakeTwo256VerifyProofVersion1() {
        byte[][] encodedNodes = new byte[][] {
            RandomGenerationUtils.generateBytes(3),
            RandomGenerationUtils.generateBytes(3)
        };
        byte[] rootHash = RandomGenerationUtils.generateBytes(3);

        byte[] scaleEncodedNodes = ScaleUtils.Encode.encode(encodedNodes);

        final var rootPointer = new RuntimePointerSize(100, TRIE_ROOT_HASH_BYTE_LEN);
        final var proofPointer = new RuntimePointerSize(101, 0);

        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(runtime.getDataFromMemory(valuePointer)).thenReturn(valueBytes);
        when(runtime.getDataFromMemory(rootPointer)).thenReturn(rootHash);
        when(runtime.getDataFromMemory(proofPointer)).thenReturn(scaleEncodedNodes);

        try (MockedStatic<TrieVerifier> mockedVerifier = mockStatic(TrieVerifier.class)) {
            mockedVerifier
                .when(() -> TrieVerifier.buildTrie(eq(encodedNodes), eq(rootHash)))
                .thenReturn(trie);

            mockedVerifier
                .when(() -> TrieVerifier.verify(eq(trie), eq(keyBytes), eq(valueBytes)))
                .thenReturn(true);

            Number proofVerified = trieHostFunctions.ext_trie_blake2_256_verify_proof_version_1(
                List.of(
                    rootPointer.pointer(),
                    proofPointer.pointerSize(),
                    keyPointer.pointerSize(),
                    valuePointer.pointerSize()
                ));

            assertEquals(1, proofVerified);

            mockedVerifier.verify(() -> TrieVerifier.verify(eq(trie), eq(keyBytes), eq(valueBytes)));
        }
    }
}