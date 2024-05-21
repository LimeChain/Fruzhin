package com.limechain.runtime.research.hybrid.context;

public class NodeStorage {
    BasicStorage localStorage;
    BasicStorage persistentStorage;
    BasicStorage baseStorage; // for offchain_index host APIs
}
