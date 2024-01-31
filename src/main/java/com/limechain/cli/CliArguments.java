package com.limechain.cli;

/**
 * Hold successfully parsed cli arguments
 *
 * @param network           the network
 * @param dbPath            the DB path
 * @param dbRecreate        flag for recreating the database for current network
 * @param nodeKey           HEX for secret Ed25519 key
 * @param noLegacyProtocols flag for disabling legacy protocols
 * @param syncMode          the sync mode
 */
public record CliArguments(String network, String dbPath, boolean dbRecreate, String nodeKey, String nodeRole,
                           boolean noLegacyProtocols, String syncMode) {
}
