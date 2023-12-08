package com.limechain.storage;

import com.limechain.chain.Chain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DBInitializerTest {
    // Isn't used directly but by making it a mock we guarantee that calls made to the static functions will be
    // redirected to the mock instead allowing us to test using verify etc.
    private final DBInitializer test = mock(DBInitializer.class);

    @AfterEach
    public void close() {
        DBInitializer.closeInstances();
    }

    // Setting private fields. Not a good idea in general
    private void setPrivateField(String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field privateField = DBInitializer.class.getDeclaredField(fieldName);
        privateField.setAccessible(true);
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (Unsafe) unsafeField.get(null);
        unsafe.putObject(unsafe.staticFieldBase(privateField), unsafe.staticFieldOffset(privateField), value);
    }

    // Accessing private fields. Not a good idea in general
    private Object getPrivateField(String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        DBInitializer initializer = mock(DBInitializer.class);
        Field privateField = DBInitializer.class.getDeclaredField(fieldName);
        privateField.setAccessible(true);

        return privateField.get(initializer);
    }

    @Test
    void initialize_addsRepository() throws NoSuchFieldException, IllegalAccessException {
        Map<String, DBRepository> instances = mock(Map.class);

        setPrivateField("INSTANCES", instances);
        String testPath = "test/path1";
        DBInitializer.initialize(testPath, Chain.WESTEND, false);

        verify(instances, times(1)).put(eq(testPath), any());
        verify(instances, never()).get(testPath);

        when(instances.containsKey(testPath)).thenReturn(true);

        DBInitializer.initialize(testPath, Chain.WESTEND, false);

        verify(instances, times(1)).get(testPath);
        verify(instances, times(1)).put(eq(testPath), any());
    }

    @Test
    void closeInstances_closesConnection() throws NoSuchFieldException, IllegalAccessException {
        Map<String, DBRepository> instances = mock(Map.class);
        String testPath1 = "test/path1";
        String testPath2 = "test/path2";
        Map.Entry<String, DBRepository> entrySet1 = new AbstractMap.SimpleEntry(testPath1, mock(DBRepository.class));
        Map.Entry<String, DBRepository> entrySet2 = new AbstractMap.SimpleEntry(testPath2, mock(DBRepository.class));

        setPrivateField("INSTANCES", instances);
        Set<Map.Entry<String, DBRepository>> set = Set.of(entrySet1, entrySet2);
        when(instances.entrySet()).thenReturn(set);

        DBInitializer.closeInstances();
        verify(entrySet1.getValue(), times(1)).closeConnection();
        verify(entrySet2.getValue(), times(1)).closeConnection();
    }

}