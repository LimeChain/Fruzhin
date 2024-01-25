package com.limechain.network;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProtocolUtils {
    public static final String PING_PROTOCOL = "/ipfs/ping/1.0.0";

    public static String getLegacyLightMessageProtocol(String genesisHash) {
        return String.format("/%s/light/2", genesisHash);
    }

    public static String getLegacyWarpSyncProtocol(String genesisHash) {
        return String.format("/%s/sync/warp", genesisHash);
    }

    public static String getLegacySyncProtocol(String genesisHash) {
        return String.format("/%s/sync/2", genesisHash);
    }

    public static String getLegacyBlockAnnounceProtocol(String genesisHash) {
        return String.format("/%s/block-announces/1", genesisHash);
    }

    public static String getLegacyKadProtocol(String chainId) {
        return String.format("/%s/kad", chainId);
    }
    public static String getGrandpaLegacyProtocol(String genesisHash) {
        return String.format("/%s/grandpa/1", genesisHash);
    }

    public static String getTransactionsProtocol(String genesisHash) {
        return String.format("/%s/transactions/1", genesisHash);
    }
}