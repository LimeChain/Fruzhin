package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.KVRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

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
    public void savesChainSpecToDB_when_chainSpecIsNotInDB() throws IOException {
//        var spec = new ChainSpec();
//        Mockito.when(hostConfig.getGenesisPath()).thenReturn("genesis/westend.json");
//
//        try (MockedStatic<ChainSpec> chainSpecStatic = Mockito.mockStatic(ChainSpec.class)) {
//            chainSpecStatic.when(() ->
//                    ChainSpec.newFromJSON("genesis/westend.json")).thenReturn(spec);
//
//            ChainService chainService = mock(ChainService.class);
//
//            doReturn(spec).when(chainService).getGenesis();
//            doReturn("genesis").when(chainService).getGenesisKey();
//
//            chainService.initialize(hostConfig);
//
//            verify(repository, times(1)).save(anyString(), any());
//
//        }
    }

}
