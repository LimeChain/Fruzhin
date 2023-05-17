package com.limechain.sync;

import com.limechain.chain.Chain;
import com.limechain.chain.ChainService;
import com.limechain.cli.CliArguments;
import com.limechain.config.HostConfig;
import com.limechain.network.Network;
import com.limechain.storage.DBInitializer;
import io.libp2p.core.Host;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyncTest {

    @Test
    public void remoteFunctions_return_correctData() {
        Host senderNode = null;

        try {
            var dbRepository = DBInitializer.initialize("./test/synctest", Chain.POLKADOT);
            CliArguments cliArguments = new CliArguments("local", "./test");
            HostConfig hostConfig = mock(HostConfig.class);
            //var hostConfig = new HostConfig(cliArguments);
            when(hostConfig.getGenesisPath()).thenReturn("genesis/westend-local.json");
            ChainService chainService = new ChainService(hostConfig, dbRepository);
            Network.initialize(chainService, hostConfig);
            Network network = Network.getInstance();
            Thread.sleep(2000);
            Sync sync = new Sync(network);
            sync.warpSync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {

            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }
}
