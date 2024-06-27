package com.limechain.runtime.hostapi;

import com.google.protobuf.ByteString;
import com.limechain.runtime.SharedMemory;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.version.StateVersion;
import com.limechain.trie.TrieStructureFactory;
import com.limechain.trie.decoded.Trie;
import com.limechain.trie.decoded.TrieVerifier;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.utils.HashUtils;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.PairReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.java.Log;
import org.javatuples.Pair;
import org.wasmer.ImportObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Implementations of the Trie HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-trie-api">Trie API</a>}
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Log
public class TrieHostFunctions implements PartialHostApi {
    static final int TRIE_ROOT_HASH_BYTE_LEN = 32;

    private final SharedMemory sharedMemory;

    @Override
    public Map<Endpoint, ImportObject.FuncImport> getFunctionImports() {
        return Map.ofEntries(
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_blake2_256_root_version_1, this::ext_trie_blake2_256_root_version_1),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_blake2_256_root_version_2, this::ext_trie_blake2_256_root_version_2),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_blake2_256_ordered_root_version_1, this::ext_trie_blake2_256_ordered_root_version_1),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_blake2_256_ordered_root_version_2, this::ext_trie_blake2_256_ordered_root_version_2),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_keccak_256_root_version_1, this::ext_trie_keccak_256_root_version_1),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_keccak_256_root_version_2, this::ext_trie_keccak_256_root_version_2),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_keccak_256_ordered_root_version_1, this::ext_trie_keccak_256_ordered_root_version_1),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_keccak_256_ordered_root_version_2, this::ext_trie_keccak_256_ordered_root_version_2),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_blake2_256_verify_proof_version_1, this::ext_trie_blake2_256_verify_proof_version_1),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_blake2_256_verify_proof_version_2, this::ext_trie_blake2_256_verify_proof_version_2),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_keccak_256_verify_proof_version_1, this::ext_trie_keccak_256_verify_proof_version_1),
            PartialHostApi.newImportObjectPair(Endpoint.ext_trie_keccak_256_verify_proof_version_2, this::ext_trie_keccak_256_verify_proof_version_2)
        );
    }

    Number ext_trie_blake2_256_root_version_1(List<Number> args) {
        log.fine("ext_trie_blake2_256_root_version_1");
        ArgParser argParser = new ArgParser(args);

        List<Pair<byte[], byte[]>> kvps = argParser.parseKeyValuePairs(0);

        byte[] trieRoot = new TrieRootCalculator(HashFunction.BLAKE2B, StateVersion.V0).trieRoot(kvps);

        return sharedMemory.writeData(trieRoot).pointer();
    }

    Number ext_trie_blake2_256_root_version_2(List<Number> argv) {
        log.fine("ext_trie_blake2_256_root_version_2");
        ArgParser argParser = new ArgParser(argv);

        List<Pair<byte[], byte[]>> kvps = argParser.parseKeyValuePairs(0);
        StateVersion stateVersion = argParser.parseStateVersion(1);

        byte[] trieRoot = new TrieRootCalculator(HashFunction.BLAKE2B, stateVersion).trieRoot(kvps);

        return sharedMemory.writeData(trieRoot).pointer();
    }

    Number ext_trie_blake2_256_ordered_root_version_1(List<Number> argv) {
        log.fine("ext_trie_blake2_256_ordered_root_version_1");
        ArgParser argParser = new ArgParser(argv);

        List<byte[]> values = argParser.parseOrderedValues(0);

        byte[] trieRoot = new TrieRootCalculator(HashFunction.BLAKE2B, StateVersion.V0).orderedTrieRoot(values);

        return sharedMemory.writeData(trieRoot).pointer();
    }

    Number ext_trie_blake2_256_ordered_root_version_2(List<Number> argv) {
        log.fine("ext_trie_blake2_256_ordered_root_version_2");
        ArgParser argParser = new ArgParser(argv);

        List<byte[]> values = argParser.parseOrderedValues(0);
        StateVersion stateVersion = argParser.parseStateVersion(1);

        byte[] trieRoot = new TrieRootCalculator(HashFunction.BLAKE2B, stateVersion).orderedTrieRoot(values);

        return sharedMemory.writeData(trieRoot).pointer();
    }

    Number ext_trie_keccak_256_root_version_1(List<Number> argv) {
        log.fine("ext_trie_keccak_256_root_version_1");
        ArgParser argParser = new ArgParser(argv);

        List<Pair<byte[], byte[]>> kvps = argParser.parseKeyValuePairs(0);

        byte[] trieRoot = new TrieRootCalculator(HashFunction.KECCAK256, StateVersion.V0).trieRoot(kvps);

        return sharedMemory.writeData(trieRoot).pointer();
    }

    Number ext_trie_keccak_256_root_version_2(List<Number> argv) {
        log.fine("ext_trie_keccak_256_root_version_2");
        ArgParser argParser = new ArgParser(argv);

        List<Pair<byte[], byte[]>> kvps = argParser.parseKeyValuePairs(0);
        StateVersion stateVersion = argParser.parseStateVersion(1);

        byte[] trieRoot = new TrieRootCalculator(HashFunction.KECCAK256, stateVersion).trieRoot(kvps);

        return sharedMemory.writeData(trieRoot).pointer();
    }

    Number ext_trie_keccak_256_ordered_root_version_1(List<Number> argv) {
        log.fine("ext_trie_keccak_256_ordered_root_version_1");
        ArgParser argParser = new ArgParser(argv);

        List<byte[]> values = argParser.parseOrderedValues(0);

        byte[] trieRoot = new TrieRootCalculator(HashFunction.KECCAK256, StateVersion.V0).orderedTrieRoot(values);

        return sharedMemory.writeData(trieRoot).pointer();
    }

    Number ext_trie_keccak_256_ordered_root_version_2(List<Number> argv) {
        log.fine("ext_trie_keccak_256_ordered_root_version_2");
        ArgParser argParser = new ArgParser(argv);

        List<byte[]> values = argParser.parseOrderedValues(0);
        StateVersion stateVersion = argParser.parseStateVersion(1);

        byte[] trieRoot = new TrieRootCalculator(HashFunction.KECCAK256, stateVersion).orderedTrieRoot(values);

        return sharedMemory.writeData(trieRoot).pointer();
    }

    Number ext_trie_blake2_256_verify_proof_version_1(List<Number> args) {
        log.fine("ext_trie_blake2_256_verify_proof_version_1");
        ArgParser argParser = new ArgParser(args);
        byte[] trieRoot = argParser.parseTrieRoot(0);
        byte[][] encodedProofNodes = argParser.parseProofNodes(1);
        byte[] key = argParser.getDataFromMemory(2);
        byte[] value = argParser.getDataFromMemory(3);
        StateVersion stateVersion = StateVersion.V0;
        HashFunction hashFunction = HashFunction.BLAKE2B;

        boolean verified = verifyProof(hashFunction, stateVersion, trieRoot, encodedProofNodes, key, value);
        return verified ? 1 : 0;
    }

    Number ext_trie_blake2_256_verify_proof_version_2(List<Number> argv) {
        log.fine("ext_trie_blake2_256_verify_proof_version_2");
        ArgParser argParser = new ArgParser(argv);
        byte[] trieRoot = argParser.parseTrieRoot(0);
        byte[][] encodedProofNodes = argParser.parseProofNodes(1);
        byte[] key = argParser.getDataFromMemory(2);
        byte[] value = argParser.getDataFromMemory(3);
        StateVersion stateVersion = argParser.parseStateVersion(4);
        HashFunction hashFunction = HashFunction.BLAKE2B;

        boolean verified = verifyProof(hashFunction, stateVersion, trieRoot, encodedProofNodes, key, value);
        return verified ? 1 : 0;
    }

    Number ext_trie_keccak_256_verify_proof_version_1(List<Number> argv) {
        log.fine("ext_trie_keccak_256_verify_proof_version_1");
        ArgParser argParser = new ArgParser(argv);
        byte[] trieRoot = argParser.parseTrieRoot(0);
        byte[][] encodedProofNodes = argParser.parseProofNodes(1);
        byte[] key = argParser.getDataFromMemory(2);
        byte[] value = argParser.getDataFromMemory(3);
        StateVersion stateVersion = StateVersion.V0;
        HashFunction hashFunction = HashFunction.KECCAK256;

        boolean verified = verifyProof(hashFunction, stateVersion, trieRoot, encodedProofNodes, key, value);
        return verified ? 1 : 0;
    }

    Number ext_trie_keccak_256_verify_proof_version_2(List<Number> args) {
        log.fine("ext_trie_keccak_256_verify_proof_version_2");
        ArgParser argParser = new ArgParser(args);
        byte[] trieRoot = argParser.parseTrieRoot(0);
        byte[][] encodedProofNodes = argParser.parseProofNodes(1);
        byte[] key = argParser.getDataFromMemory(2);
        byte[] value = argParser.getDataFromMemory(3);
        StateVersion stateVersion = argParser.parseStateVersion(4);
        HashFunction hashFunction = HashFunction.KECCAK256;

        boolean verified = verifyProof(hashFunction, stateVersion, trieRoot, encodedProofNodes, key, value);
        return verified ? 1 : 0;
    }

    private boolean verifyProof(HashFunction hashFunction, StateVersion version, byte[] trieRoot,
                                byte[][] encodedProofNodes, byte[] key, byte[] value) {
        // TODO: Figure out how the state version affects proof verification
        try {
            Trie trie = TrieVerifier.buildTrie(encodedProofNodes, trieRoot, hashFunction.getFunction());
            TrieVerifier.verify(trie, key, value);
        } catch (RuntimeException e) {
            return false;
        }

        return true;
    }

    @Getter
    @AllArgsConstructor
    enum HashFunction {
        BLAKE2B(HashUtils::hashWithBlake2b),
        KECCAK256(HashUtils::hashWithKeccak256);

        private static final byte[] EMPTY_BLAKE2_TRIE_MERKLE_VALUE =
                {3, 23, 10, 46, 117, -105, -73, -73, -29, -40, 76, 5, 57, 29, 19, -102, 98, -79, 87, -25, -121, -122, -40,
                        -64, -126, -14, -99, -49, 76, 17, 19, 20};
        private static final byte[] EMPTY_KECCAK256_TRIE_MERKLE_VALUE =
                {-68, 54, 120, -98, 122, 30, 40, 20, 54, 70, 66, 41, -126, -113, -127, 125, 102, 18, -9, -76, 119, -42, 101,
                        -111, -1, -106, -87, -32, 100, -68, -55, -118};
        private final UnaryOperator<byte[]> function;

        public byte[] getEmptyTrieHash() {
            return switch (this) {
                case BLAKE2B -> EMPTY_BLAKE2_TRIE_MERKLE_VALUE;
                case KECCAK256 -> EMPTY_KECCAK256_TRIE_MERKLE_VALUE;
            };
        }
    }

    record TrieRootCalculator(HashFunction hashFunction, StateVersion stateVersion) {
        public byte[] trieRoot(List<Pair<byte[], byte[]>> entries) {
            Map<ByteString, ByteString> entriesMap = entries.stream().collect(
                    Collectors.toMap(p -> ByteString.copyFrom(p.getValue0()), p -> ByteString.copyFrom(p.getValue1())));
            return trieRoot(entriesMap);
        }

        public byte[] orderedTrieRoot(List<byte[]> values) {
            Map<ByteString, ByteString> entries = new HashMap<>();

            int i = 0;
            for (byte[] value : values) {
                byte[] key = ScaleUtils.Encode.encode(ScaleCodecWriter::writeCompact, i++);
                entries.put(ByteString.copyFrom(key), ByteString.copyFrom(value));
            }

            return trieRoot(entries);
        }

        private byte[] trieRoot(Map<ByteString, ByteString> entries) {
            var trie = TrieStructureFactory.buildTrieStructure(entries);
            TrieStructureFactory.calculateMerkleValues(trie, hashFunction.getFunction());
            return trie.getRootNode().map(NodeHandle::getUserData).map(NodeData::getMerkleValue)
                    .orElse(hashFunction.getEmptyTrieHash());
        }
    }

    @AllArgsConstructor
    class ArgParser {
        private final List<Number> args;

        public StateVersion parseStateVersion(int index) {
            return StateVersion.fromInt((Integer) args.get(index));
        }

        public List<byte[]> parseOrderedValues(int index) {
            return ScaleUtils.Decode.decodeList(getDataFromMemory(index), ScaleCodecReader::readByteArray);
        }

        public List<Pair<byte[], byte[]>> parseKeyValuePairs(int index) {
            return ScaleUtils.Decode.decodeList(getDataFromMemory(index),
                    new PairReader<>(ScaleCodecReader::readByteArray, ScaleCodecReader::readByteArray));
        }

        public byte[][] parseProofNodes(int index) {
            return ScaleUtils.Decode.decodeList(getDataFromMemory(index), ScaleCodecReader::readByteArray)
                    .toArray(new byte[0][0]);
        }

        public byte[] parseTrieRoot(int index) {
            int rootPtr = args.get(index).intValue();
            return sharedMemory.readData(new RuntimePointerSize(rootPtr, TRIE_ROOT_HASH_BYTE_LEN));
        }

        public byte[] getDataFromMemory(int index) {
            return sharedMemory.readData(new RuntimePointerSize(args.get(index)));
        }
    }
}
