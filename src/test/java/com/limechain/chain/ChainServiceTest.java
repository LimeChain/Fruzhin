package com.limechain.chain;

import com.limechain.chain.spec.ChainSpec;
import com.limechain.config.HostConfig;
import com.limechain.storage.KVRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChainServiceTest {
    private ChainService chainService;
    private HostConfig hostConfig;
    private KVRepository<String, Object> repository;

    @BeforeEach
    public void setup() {
        hostConfig = mock(HostConfig.class);
        repository = mock(KVRepository.class);
    }

    @Test
    void setsChainSpecFromDB_when_chainSpecIsInDB() {
        var chainSpec = mock(ChainSpec.class);
        when(repository.find(any())).thenReturn(Optional.of(chainSpec));
        chainService = new ChainService(hostConfig, repository);
        assertEquals(chainSpec, chainService.getChainSpec());
    }

    @Test
    void savesChainSpecToDB_when_chainSpecIsNotInDB() {
        var chainSpec = new ChainSpec();

        doReturn(Optional.empty()).when(repository).find(any());

        try (MockedStatic<ChainSpec> chainSpecStatic = Mockito.mockStatic(ChainSpec.class)) {
            chainSpecStatic
                .when(() -> ChainSpec.newFromJSON(any()))
                .thenReturn(chainSpec);

            chainService = new ChainService(hostConfig, repository);
            verify(repository, times(1)).save("genesis", chainSpec);
        }
    }
}
