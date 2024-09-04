package com.limechain.network;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProtocolUtils {
    public static final String PING_PROTOCOL = "/ipfs/ping/1.0.0";

    public static String getLightMessageProtocol(String chainId) {
        return String.format("/%s/light/2", chainId);
    }

    public static String getWarpSyncProtocol(String chainId) {
        return String.format("/%s/sync/warp", chainId);
    }

    public static String getSyncProtocol(String chainId) {
        return String.format("/%s/sync/2", chainId);
    }

    public static String getStateProtocol(String chainId) {
        return String.format("/%s/state/2", chainId);
    }

    public static String getBlockAnnounceProtocol(String chainId) {
        return String.format("/%s/block-announces/1", chainId);
    }

    public static String getKadProtocol(String chainId) {
        return String.format("/%s/kad", chainId);
    }

    public static String getGrandpaProtocol(String chainId, boolean legacyProtocol) {
        return String.format("/%s/grandpa/1", legacyProtocol ? "paritytech" : chainId);
    }

    public static String getGrandpaProtocol(String chainId) {
        return String.format("/%s/grandpa/1", grandpaProtocolChain(chainId));
    }

    //TODO: figure out a more elegant solution
    private static String grandpaProtocolChain(String chainId) {
        return chainId.equals("dot") ? "paritytech" : chainId;
    }

    public static String getTransactionsProtocol(String chainId) {
        return String.format("/%s/transactions/1", chainId);
    }
}