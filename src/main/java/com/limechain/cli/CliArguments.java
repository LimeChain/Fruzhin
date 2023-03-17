package com.limechain.cli;

/**
 * Hold successfully parsed cli arguments
 *
 * @param network the network
 * @param dbPath  the DB path
 */
public record CliArguments(String network, String dbPath) {
}
