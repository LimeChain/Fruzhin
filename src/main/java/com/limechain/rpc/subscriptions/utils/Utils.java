package com.limechain.rpc.subscriptions.utils;

/**
 * Stores function that don't belong to any specific class
 */
public class Utils {

    /**
     * Wraps a string with double quotes. Used when passing a jsonrpc parameter in the request body
     *
     * @param value the value to wrap
     * @return the wrapped string
     */
    public static String wrapWithDoubleQuotes(String value) {
        return '"' + value + '"';
    }

}
