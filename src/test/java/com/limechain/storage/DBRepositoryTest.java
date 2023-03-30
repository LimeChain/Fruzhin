package com.limechain.storage;

import com.limechain.chain.Chain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DBRepositoryTest {
    private DBRepository dbRepository;

    @BeforeEach
    public void setup() {
        dbRepository = DBInitializer.initialize("./test", Chain.WESTEND);
    }

    @AfterEach
    public void close() {
        dbRepository = null;
    }

    @Test
    public void find_returnsEmpty_whenKeyDoesNotExist() {
        Optional<Object> result = dbRepository.find("empty-key");

        assertFalse(result.isPresent());
    }

    @Test
    public void findSave_returnsValue_whenKeyExists() {
        boolean saveResult = dbRepository.save("key1", "value1");
        assertTrue(saveResult);

        Optional<Object> value = dbRepository.find("key1");

        assertTrue(value.isPresent());
        assertEquals(value.get(), "value1");
    }

    @Test
    public void del_deletesValue_whenKeyExists() {
        boolean saveResult = dbRepository.save("key1", "value1");
        assertTrue(saveResult);

        Optional<Object> findValue = dbRepository.find("key1");

        assertTrue(findValue.isPresent());
        assertEquals(findValue.get(), "value1");

        boolean delResult = dbRepository.delete("key1");
        assertTrue(delResult);

        Optional<Object> delValue = dbRepository.find("key1");

        assertFalse(delValue.isPresent());

    }
}
