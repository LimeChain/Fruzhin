package com.limechain.storage.offchain;

import com.limechain.storage.KVRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffchainStoreTest {
    private static final String TEST_PREFIX = "test_";
    private final String key = "key";
    private final String prefixedKey = TEST_PREFIX.concat(key);

    private OffchainStore offchainStore;

    @Mock
    private KVRepository<String, Object> repository;

    @BeforeEach
    void setUp() {
        offchainStore = new OffchainStore(repository, TEST_PREFIX);
    }

    @Test
    void setShouldPrefixKeyAndPersist() {
        byte[] value = new byte[] {1,2,3};

        offchainStore.set(key, value);

        verify(repository).save(prefixedKey, value);
    }

    @Test
    void getShouldFindPrefixedKeyInRepository() {
        byte[] value = new byte[] {1,2,3};
        when(repository.find(prefixedKey)).thenReturn(Optional.of(value));

        byte[] actual = offchainStore.get(key);

        Assertions.assertEquals(value, actual);
    }

    @Test
    void removeShouldPrefixKeyAndRemoveItFromRepository() {
        offchainStore.remove(key);

        verify(repository).delete(prefixedKey);
    }

    @Test
    void compareAndSetWhenOldValueMatchesStoredShouldSetNewValue() {
        byte[] oldValue = new byte[] {1, 2, 3};
        byte[] newValue = new byte[] {4, 5, 6};
        when(repository.find(prefixedKey)).thenReturn(Optional.of(oldValue));

        boolean success = offchainStore.compareAndSet(key, oldValue, newValue);

        Assertions.assertTrue(success);
        verify(repository).save(prefixedKey, newValue);
    }

    @Test
    void compareAndSetWhenOldValueDoesntMatchStoredShouldIgnoreAndReturnFalse() {
        byte[] oldValue = new byte[] {1, 2, 3};
        byte[] repositoryValue = new byte[] {4, 5, 6};
        byte[] newValue = new byte[] {7, 8, 9};

        when(repository.find(prefixedKey)).thenReturn(Optional.of(repositoryValue));

        boolean success = offchainStore.compareAndSet(key, oldValue, newValue);

        Assertions.assertFalse(success);
        verifyNoMoreInteractions(repository);
    }
}