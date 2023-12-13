package com.limechain.network;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProtocolUtils {
    public static final String PING_PROTOCOL = "/ipfs/ping/1.0.0";
    public static final String LEGACY_GRANDPA_PROTOCOL = "/paritytech/grandpa/1";

    public static String getLegacyLightMessageProtocol(String chainId) {
        return String.format("/%s/light/2", chainId);
    }

    public static String getLegacyWarpSyncProtocol(String chainId) {
        return String.format("/%s/sync/warp", chainId);
    }

    public static String getLegacySyncProtocol(String chainId) {
        return String.format("/%s/sync/2", chainId);
    }

    public static String getLegacyBlockAnnounceProtocol(String chainId) {
        return String.format("/%s/block-announces/1", chainId);
    }

    public static String getLegacyKadProtocol(String chainId) {
        return String.format("/%s/kad", chainId);
    }

    public static String getTransactionsProtocol(String chainId) {
        return String.format("/%s/transactions/1", chainId);
    }
}