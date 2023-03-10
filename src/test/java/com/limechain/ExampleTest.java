package com.limechain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.limechain.Example.intAddition;

class ExampleTest {
    @Test
    void testIntAddition() {
        assertEquals(6, intAddition(2, 3));
    }
}
