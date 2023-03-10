package com.limechain;

import org.junit.jupiter.api.Test;

import static com.limechain.Example.IntAddition;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExampleTest {
    @Test
    void TestIntAddition() {
        assertEquals(5, IntAddition(2, 3));
    }
}
