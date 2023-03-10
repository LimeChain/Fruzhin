package com.limechain;

import org.junit.jupiter.api.Test;

import static com.limechain.Example.intAddition;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExampleTest {
    @Test
    void TestIntAddition() {
        assertEquals(5, intAddition(2, 3));
    }
}
