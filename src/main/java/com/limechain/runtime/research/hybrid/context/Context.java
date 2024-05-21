package com.limechain.runtime.research.hybrid.context;

import com.limechain.runtime.research.hybrid.hostapi.SharedMemory;
import com.limechain.runtime.research.hybrid.trieaccessor.TrieAccessor;
import com.limechain.storage.crypto.KeyStore;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Context {
    TrieAccessor trieAccessor;
    KeyStore keyStore;
    NodeStorage nodeStorage;
    boolean isValidator;

    // Inner context fields (i.e. dependent on the `org.wasmer.Instance`), manipulated by the runtime for proper endpoint execution
    @Setter
    SharedMemory sharedMemory;

    public Context(TrieAccessor trieAccessor, KeyStore keyStore, NodeStorage nodeStorage, boolean isValidator) {
        this.trieAccessor = trieAccessor;
        this.keyStore = keyStore;
        this.nodeStorage = nodeStorage;
        this.isValidator = isValidator;
    }
}
