package com.limechain.lightclient;

import com.limechain.network.Network;
import com.limechain.rpc.server.RpcApp;
import com.limechain.sync.warpsync.WarpSyncMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LightClientTest {
    private LightClient lightClient;

    private RpcApp rpcApp;
    private String[] args;

    // Setting private fields. Not a good idea in general
    private void setPrivateField(String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field privateField = LightClient.class.getDeclaredField(fieldName);
        privateField.setAccessible(true);

        privateField.set(lightClient, value);
    }

    @BeforeEach
    public void setup() {
        rpcApp = mock(RpcApp.class);
        args = new String[]{"some args"};

        lightClient = new LightClient(args, rpcApp);
    }

    @Test
    public void lightClient_stop_invokesStopFunctions() throws NoSuchFieldException, IllegalAccessException {
        Network network = mock(Network.class);
        WarpSyncMachine warpSync = mock(WarpSyncMachine.class);

        setPrivateField("network", network);
        setPrivateField("warpSyncMachine", warpSync);

        lightClient.stop();

        verify(rpcApp, times(1)).stop();

    }
}
