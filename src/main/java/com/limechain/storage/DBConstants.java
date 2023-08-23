package com.limechain.storage;

public class DBConstants {
    /**
    * Key for storing the privateKey for nabu
    *  */
    public static final String PEER_ID = "nodePeerId";
    /**
     * Key under which the genesis chain spec is stored
     */
    public static final String GENESIS_KEY = "genesis";
    /**
     * Key under which the latest sync state is stored
     */
    public static final String SYNC_STATE_KEY = "syncState";
    /**
     * Key under which the --latest-- state trie proof is stored
     * TODO: Currently only the latest loaded in sync is stored
     */
    public static final String STATE_TRIE_MERKLE_PROOF = "stateTrieProof";
    /**
     * Key under which the --latest-- state trie root state is stored
     * TODO: Currently only the latest loaded in sync is stored
     */
    public static final String STATE_TRIE_ROOT_HASH = "stateTrieRootState";

}
