package com.limechain.storage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DBConstants {
    /**
     * Key for storing the privateKey for nabu
     */
    public static final String PEER_ID = "nodePeerId";
    /**
     * Key under which the genesis chain spec is stored
     */
    public static final String GENESIS_KEY = "genesis";
    /**
     * Key under which the --latest-- state trie proof is stored
     * TODO: Currently only the latest loaded in sync is stored
     */
    public static final String RUNTIME_CODE = "runtimeCode";

    /**
     * Key under which the hash of the latest finalised block header is stored.
     */
    public static final String FINALIZED_BLOCK_KEY = "finalised_head";

    /**
     * Key under which the highest round and set id is stored.
     */
    public static final String HIGHEST_ROUND_AND_SET_ID_KEY = "hrs";

    // SyncState keys
    public static final String LAST_FINALIZED_BLOCK_NUMBER = "ss::lastFinalizedBlockNumber";
    public static final String LAST_FINALIZED_BLOCK_HASH = "ss::lastFinalizedBlockHash";
    public static final String STATE_ROOT = "ss::stateRoot";

    // GrandpaState keys
    public static final String GRANDPA_AUTHORITY_SET = "gs::grandpaAuthoritySet";
    public static final String GRANDPA_SET_ID = "gs::grandpaSetId";
    public static final String LATEST_ROUND = "gs::latestRound";
    public static final String GRANDPA_PREVOTES = "gs:grandpaPreVotes";
    public static final String GRANDPA_PRECOMMITS = "gs:grandpaPreCommits";

    //BeefyState keys
    public static final String BEEFY_AUTHORITY_SET = "gs::beefyAuthoritySet";
    public static final String BEEFY_SET_ID = "gs::beefySetId";
    public static final String BEEFY_FINALIZED = "bs:beefyFinalized";
    public static final String BEEFY_GRANDPA_FINALIZED = "bs:grandpaFinalized";
    public static final String BEEFY_ROUND = "bs:beefyRound";
    public static final String BEEFY_GENESIS = "bs:beefyGenesis";
    public static final String BEEFY_LAST_VOTED = "bs:lastVoted";
    public static final String BEEFY_SESSIONS = "bs:sessions";
    public static final String BEEFY_JUSTIFICATION = "bs:justification";
}
