package com.limechain.rpc.methods.offchain;

import com.limechain.storage.KVRepository;
import com.limechain.storage.offchain.StorageKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OffchainRPCImplTest {
    @Mock
    private KVRepository<String, Object> dbMock;
    @InjectMocks
    private OffchainRPCImpl offchainRPC;

    @Test
    void testOffchainLocalStorageSetPersistent() {
        String key = "0x74657374";
        String value = "0x5678";

        String keyStr = "test";
        byte[] valueBytes = new byte[]{0x56, 0x78};

        offchainRPC.offchainLocalStorageSet(StorageKind.PERSISTENT, key, value);

        verify(dbMock).save("offchain_persistent_" + keyStr, valueBytes);
    }

    @Test
    void testOffchainLocalStorageSetLocal() {
        String key = "0x74657374";
        String value = "0xcdef";

        String keyStr = "test";
        byte[] valueBytes = new byte[]{(byte) 0xCD, (byte) 0xEF};

        offchainRPC.offchainLocalStorageSet(StorageKind.LOCAL, key, value);

        verify(dbMock).save("offchain_local_" + keyStr, valueBytes);
    }

    @Test
    void testOffchainLocalStorageGetPersistent() {
        String key = "0x74657374";

        String keyStr = "test";
        byte[] returnValue = new byte[]{0x56, 0x78};
        String expectedValue = "0x5678";

        doReturn(Optional.of(returnValue)).when(dbMock).find("offchain_persistent_" + keyStr);

        String result = offchainRPC.offchainLocalStorageGet(StorageKind.PERSISTENT, key);

        assertEquals(expectedValue, result);
    }

    @Test
    void testOffchainLocalStorageGetLocal() {
        String key = "0x74657374";

        String keyStr = "test";
        byte[] returnValue = new byte[]{(byte) 0xCD, (byte) 0xEF};
        String expectedValue = "0xcdef";

        doReturn(Optional.of(returnValue)).when(dbMock).find("offchain_local_" + keyStr);

        String result = offchainRPC.offchainLocalStorageGet(StorageKind.LOCAL, key);

        assertEquals(expectedValue, result);
    }

}