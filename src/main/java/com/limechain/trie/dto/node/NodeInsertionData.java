package com.limechain.trie.dto.node;

import com.limechain.runtime.version.StateVersion;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class NodeInsertionData {

    Nibbles key;
    byte[] newNodeValue;
    StateVersion newNodeStateVersion;
    @Nullable
    TraversedNode ancestor;
}
