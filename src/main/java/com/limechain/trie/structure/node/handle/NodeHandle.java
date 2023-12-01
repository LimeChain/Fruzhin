package com.limechain.trie.structure.node.handle;

import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.node.TrieNodeIndex;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public abstract sealed class NodeHandle<T> permits StorageNodeHandle, BranchNodeHandle {
    private TrieStructure<T> trieStructure;
    private int nodeIndex;

    //NOTE:
    // Later on, we might switch from optionals to nullable return types
    // Depends on whether Optionals will be more helpful or more annoying
    public Optional<NodeHandle<T>> getChild(Nibble index) {
        //TODO: implement
        throw new NotImplementedException("Not yet implemented.");
    }

    public List<Nibble> getPartialKey() {
        //TODO: implement
        throw new NotImplementedException("Not yet implemented.");
    }

    public T getUserData() {
        return trieStructure.nodeAtIndex(new TrieNodeIndex(nodeIndex)).getUserData();
    }
}
