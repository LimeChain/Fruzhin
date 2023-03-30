package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.KVRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ChainServiceTest {
    private ChainService chainService;
    private HostConfig hostConfig;
    private KVRepository<String, Object> repository;

    @BeforeEach
    public void setup() {
        hostConfig = mock(HostConfig.class);
        repository = mock(KVRepository.class);
    }

    @Test
    public void setsChainSpecFromDB_when_chainSpecIsInDB() {
        var chainSpec = new ChainSpec() {{
            this.setName("testName");
        }};
        Optional<Object> mockGenesis = Optional.of(chainSpec);

        doReturn(mockGenesis).when(repository).find(any());

        chainService = new ChainService(hostConfig, repository);

        assertEquals(chainService.getGenesis(), chainSpec);
    }

    @Test
    public void savesChainSpecToDB_when_chainSpecIsNotInDB() {
        var chainSpec = new ChainSpec() {{
            this.setName("testName");
        }};

        Optional<Object> mockGenesis = Optional.ofNullable(null);

        doReturn(mockGenesis).when(repository).find(any());

        try (MockedStatic<ChainSpec> chainSpecStatic = Mockito.mockStatic(ChainSpec.class)) {
            chainSpecStatic.when(() ->
                    ChainSpec.newFromJSON(any())).thenReturn(chainSpec);

            chainService = new ChainService(hostConfig, repository);
            verify(repository, times(1)).save("genesis", chainSpec);
        }
    }


    @Test
    public void throwsRuntimeException_when_saveFails() {
        Optional<Object> mockGenesis = Optional.ofNullable(null);

        doReturn(mockGenesis).when(repository).find(any());

        try (MockedStatic<ChainSpec> chainSpecStatic = Mockito.mockStatic(ChainSpec.class)) {
            chainSpecStatic.when(() ->
                    ChainSpec.newFromJSON(any())).thenThrow(IOException.class);

            assertThrows(RuntimeException.class, () -> chainService = new ChainService(hostConfig, repository));
        }
    }

}
