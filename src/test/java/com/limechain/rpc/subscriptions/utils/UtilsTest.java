package com.limechain.rpc.subscriptions.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class UtilsTest {

    @Test
    void wrapWithDoubleQuotes_worksCorrect() {
        assertEquals("\"\"", Utils.wrapWithDoubleQuotes(""));
        assertEquals("\"1\"", Utils.wrapWithDoubleQuotes("1"));
        assertEquals("\"0x123\"", Utils.wrapWithDoubleQuotes("0x123"));
        assertEquals("\"some_string\"", Utils.wrapWithDoubleQuotes("some_string"));
    }
}