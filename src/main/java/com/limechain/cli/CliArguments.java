package com.limechain.cli;

/**
 * Hold successfully parsed cli arguments
 *
 * @param network the network
 * @param dbPath  the DB path
 * @param dbRecreate flag for recreating the database for current network
 * @param nodeKey HEX for secret Ed25519 key
 */
public record CliArguments(String network, String dbPath, boolean dbRecreate, String nodeKey) {
}
