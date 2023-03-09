package com.limechain.lightclient;

import com.limechain.rpc.http.server.HttpRpc;
import com.limechain.rpc.ws.server.WebSocketRPC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LightClientTest {
    private LightClient lightClient;

    private HttpRpc httpRpc;
    private WebSocketRPC wsRpc;
    private String[] args;

    @BeforeEach
    public void setup() {
        httpRpc = mock(HttpRpc.class);
        wsRpc = mock(WebSocketRPC.class);
        args = new String[]{"some args"};

        lightClient = new LightClient(args, httpRpc, wsRpc);
    }

    @Test
    public void lightClient_start_invokesRpcStartFunctions() {
        lightClient.start();

        verify(httpRpc, times(1)).start(args);
        verify(wsRpc, times(1)).start(args);
    }

    @Test
    public void lightClient_stop_invokesRpcStopFunctions() {
        lightClient.stop();

        verify(httpRpc, times(1)).stop();
        verify(wsRpc, times(1)).stop();
    }

}
