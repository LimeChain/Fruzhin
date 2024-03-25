package com.limechain.rpc.methods.state;

import com.limechain.rpc.methods.state.dto.StorageChangeSet;
import com.limechain.runtime.Runtime;
import com.limechain.storage.block.BlockState;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.BlockTrieAccessor;
import com.limechain.trie.dto.node.StorageNode;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.types.Hash256;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class StateRPCImpl {

    private final TrieStorage trieStorage = TrieStorage.getInstance();
    private final BlockState blockState = BlockState.getInstance();

    public void stateCall(final String method, final String data, final String blockHashHex) {
        throw new UnsupportedOperationException("This API is future-reserved.");
    }

    public String[][] stateGetPairs(final String prefixHex, final String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return new String[0][0];
        }

        byte[] prefix = StringUtils.hexToBytes(prefixHex);
        final Hash256 blockHash =
                blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();

        Optional<StorageNode> optionalNextBranch = trieStorage.getNextBranch(blockHash, new String(prefix));
        if (optionalNextBranch.isEmpty()) {
            return new String[0][0];
        }
        StorageNode nextBranch = optionalNextBranch.get();
        byte[] nextBranchMerkle = nextBranch.nodeData().getMerkleValue();

        return trieStorage
                .loadChildren(nextBranch.key(), nextBranchMerkle)
                .stream()
                .map(storageNode -> {
                    final String key = storageNode.key().toLowerHexString();
                    final String value = HexUtils.toHexString(storageNode.nodeData().getValue());
                    return new String[]{key, value};
                })
                .toArray(String[][]::new);
    }

    public String[][] stateGetKeysPaged(final String prefixHex, int limit, String keyHex, final String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return new String[0][0];
        }

        byte[] prefix = prefixHex != null ? StringUtils.hexToBytes(prefixHex) : new byte[0];
        byte[] startKey = keyHex != null ? StringUtils.hexToBytes(keyHex) : new byte[0];
        final Hash256 blockHash =
                blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();
        List<byte[]> keys = trieStorage.getKeysWithPrefixPaged(blockHash, prefix, startKey, limit);

        String[][] result = new String[keys.size()][];
        for (int i = 0; i < keys.size(); i++) {
            result[i] = new String[]{HexUtils.toHexString(keys.get(i))};
        }

        return result;
    }

    public String stateGetStorage(final String keyHex, final String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return null;
        }

        byte[] key = StringUtils.hexToBytes(keyHex);
        final Hash256 blockHash =
                blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();

        return trieStorage.getByKeyFromBlock(blockHash, new String(key))
                .map(NodeData::getValue)
                .map(HexUtils::toHexString)
                .orElse(null);
    }

    public String stateGetStorageHash(final String keyHex, final String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return null;
        }

        byte[] key = StringUtils.hexToBytes(keyHex);
        final Hash256 blockHash =
                blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();

        return trieStorage.getByKeyFromBlock(blockHash, new String(key))
                .map(NodeData::getMerkleValue)
                .map(HexUtils::toHexString)
                .orElse(null);
    }

    public String stateGetStorageSize(final String keyHex, final String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return null;
        }

        byte[] key = StringUtils.hexToBytes(keyHex);
        final Hash256 blockHash =
                blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();

        return trieStorage
                .getByKeyFromBlock(blockHash, new String(key))
                .map(NodeData::getValue)
                .map(Array::getLength)
                .map(String::valueOf)
                .orElse(null);
    }

    public String stateGetMetadata(final String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return null;
        }

        final Hash256 blockHash =
                blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();

        final Runtime runtime = blockState.getRuntime(blockHash);
        byte[] metadataBytes = runtime.call("Metadata_metadata");

        return HexUtils.toHexString(metadataBytes);
    }

    public String stateGetRuntimeVersion(final String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return null;
        }

        final Hash256 blockHash =
                blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();

        Runtime runtime = blockState.getRuntime(blockHash);
        if (runtime != null) {
            return runtime.getVersion().toString();
        }
        return null;
    }

    public List<StorageChangeSet> stateQueryStorage(final List<String> keyHex, final String startBlockHex,
                                                    final String endBlockHex) {
        if (!this.blockState.isInitialized()) {
            return Collections.emptyList();
        }

        final Hash256 startBlockHash = Hash256.from(startBlockHex);
        final Hash256 endBlockHash =
                endBlockHex != null ? Hash256.from(endBlockHex) : blockState.getHighestFinalizedHash();

        final List<StorageChangeSet> changesPerBlock = new ArrayList<>();
        final Map<String, String> previousValues = new HashMap<>();
        for (Hash256 blockHash : blockState.range(startBlockHash, endBlockHash)) {
            final Map<String, String> changes = new HashMap<>();
            for (String key : keyHex) {
                byte[] keyBytes = StringUtils.hexToBytes(key);

                final Optional<String> currentValueOpt = trieStorage.getByKeyFromBlock(blockHash, new String(keyBytes))
                        .map(NodeData::getValue)
                        .map(HexUtils::toHexString);

                final String currentValue = currentValueOpt.orElse(null);
                final String previousValue = previousValues.get(key);

                if (!Objects.equals(currentValue, previousValue)) {
                    changes.put(key, currentValue);
                    previousValues.put(key, currentValue);
                }
            }
            if (!changes.isEmpty()) {
                changesPerBlock.add(new StorageChangeSet(blockHash.toString(), changes));
            }
        }

        return changesPerBlock;
    }

    public Map<String, Object> stateGetReadProof(final List<String> keyHexList, final String blockHashHex) {
        final Hash256 blockHash =
                blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();

        BlockTrieAccessor blockTrieAccessor = new BlockTrieAccessor(blockHash);
        List<String> readProof = keyHexList
                .stream()
                .map(StringUtils::hexToBytes)
                .map(key -> blockTrieAccessor
                        .findMerkleValue(Nibbles.fromBytes(key))
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(HexUtils::toHexString)
                .toList();

        return Map.of(
                "at", HexUtils.toHexString(blockHash.getBytes()),
                "proof", readProof
        );
    }

}