package com.limechain.cli;

/**
 * Hold successfully parsed cli arguments
 *
 * @param network the network
 * @param dbPath  the DB path
 * @param dbRecreate flag for recreating the database for current network
 */
public record CliArguments(String network, String dbPath, boolean dbRecreate) {
}
