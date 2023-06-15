package com.limechain.lightclient;

import com.limechain.network.Network;
import com.limechain.rpc.http.server.AppBean;
import com.limechain.rpc.http.server.HttpRpc;
import com.limechain.rpc.ws.server.WebSocketRPC;
import com.limechain.sync.warpsync.WarpSyncMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LightClientTest {
    private LightClient lightClient;

    private HttpRpc httpRpc;
    private WebSocketRPC wsRpc;
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
        httpRpc = mock(HttpRpc.class);
        wsRpc = mock(WebSocketRPC.class);
        args = new String[]{"some args"};

        lightClient = new LightClient(args, httpRpc, wsRpc);
    }

    @Test
    public void lightClient_start_invokesStartFunctions() {
        Network network = mock(Network.class);
        WarpSyncMachine warpSync = mock(WarpSyncMachine.class);
        try (MockedStatic<AppBean> utilities = Mockito.mockStatic(AppBean.class)) {
            utilities.when(() -> AppBean.getBean(Network.class)).thenReturn(network);
            utilities.when(() -> AppBean.getBean(WarpSyncMachine.class)).thenReturn(warpSync);

            lightClient.start();

            verify(httpRpc, times(1)).start(args);
            verify(wsRpc, times(1)).start(args);
            verify(network, times(1)).start();
            verify(warpSync, times(1)).start();
        }
    }

    @Test
    public void lightClient_stop_invokesStopFunctions() throws NoSuchFieldException, IllegalAccessException {
        Network network = mock(Network.class);
        WarpSyncMachine warpSync = mock(WarpSyncMachine.class);

        setPrivateField("network", network);
        setPrivateField("warpSyncService", warpSync);

        lightClient.stop();

        verify(httpRpc, times(1)).stop();
        verify(wsRpc, times(1)).stop();
    }
}
