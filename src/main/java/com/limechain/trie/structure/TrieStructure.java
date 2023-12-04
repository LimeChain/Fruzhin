package com.limechain.trie.structure;

import com.limechain.trie.structure.node.TrieNodeIndex;
import com.limechain.trie.structure.node.handle.NodeHandle;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class TrieStructure<T> implements Iterable<TrieNodeIndex> {

    // INTERFACE
    @NotNull
    @Override
    public Iterator<TrieNodeIndex> iterator() {
        //TODO: implement
        throw new NotImplementedException("Not yet implemented.");
    }

    public T getUserDataAtIndex(TrieNodeIndex index) {
        //TODO: implement
        throw new NotImplementedException("Not yet implemented.");
    }

    public NodeHandle<T> nodeAtIndex(TrieNodeIndex index) {
        //TODO: implement
        throw new NotImplementedException("Not yet implemented.");
    }
    // END OF INTERFACE
}
