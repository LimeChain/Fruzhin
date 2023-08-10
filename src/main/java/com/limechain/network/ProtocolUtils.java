package com.limechain.network;

public final class ProtocolUtils {
    public static String getPingProtocol() {
        return "/ipfs/ping/1.0.0";
    }

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

    public static String getGrandpaLegacyProtocol() {
        return "/paritytech/grandpa/1";
    }
}