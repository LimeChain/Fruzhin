package com.limechain.trie.structure;

import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class NodeData {
    @Nullable
    private byte[] value;
    @Nullable
    private byte[] merkleValue;

    public NodeData(@Nullable byte[] value) {
        this.value = value;
    }
}
