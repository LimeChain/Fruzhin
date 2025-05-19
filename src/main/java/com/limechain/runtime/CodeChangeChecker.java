package com.limechain.runtime;

import com.limechain.exception.global.RuntimeCodeException;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import com.limechain.trie.TrieAccessor;
import com.limechain.trie.TrieAccessorStorage;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodeChangeChecker {

    private final StateManager stateManager;
    private final RuntimeBuilder runtimeBuilder;
    private final TrieAccessorStorage trieAccessorStorage;

    private String storedCodeHex;

    public void initStoredCode(byte[] code) {
        if (storedCodeHex != null) {
            throw new RuntimeCodeException("Code has already been initialized.");
        }

        storedCodeHex = StringUtils.toHexWithPrefix(code);
    }

    public Optional<Runtime> checkRuntimeCodeChange(BlockHeader header) {
        BlockState blockState = stateManager.getBlockState();
        Hash256 hash = header.getHash();
        Runtime parentRuntime = blockState.getRuntime(header.getParentHash());

        Optional<byte[]> optRuntimeCode = parentRuntime.getRuntimeCode(header);

        if (optRuntimeCode.isEmpty()) {
            throw new RuntimeCodeException("Runtime code should not be empty");
        }

        String runtimeCodeHex = StringUtils.toHexWithPrefix(optRuntimeCode.get());
        if (runtimeCodeHex.equals(storedCodeHex)) {
            return Optional.empty();
        }

        TrieAccessor trieAccessor = trieAccessorStorage.get(hash);

        Runtime newRuntime = runtimeBuilder.buildRuntime(optRuntimeCode.get(), trieAccessor);
        trieAccessor.setCurrentStateVersion(newRuntime.getCachedVersion().getStateVersion());
        blockState.storeRuntime(hash, newRuntime);

        storedCodeHex = runtimeCodeHex;

        return Optional.of(newRuntime);
    }
}
