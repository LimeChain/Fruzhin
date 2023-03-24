package com.limechain.rpc.ws.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.limechain.rpc.config.SubscriptionName;
import com.limechain.rpc.methods.RPCMethods;
import com.limechain.rpc.pubsub.PubSubService;
import com.limechain.rpc.pubsub.Topic;
import com.limechain.rpc.subscriptions.chainhead.ChainHeadRpc;
import com.limechain.rpc.subscriptions.transaction.TransactionRpc;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;

@Component
@Log
public class WebSocketHandler extends TextWebSocketHandler {

    private final JsonRpcBasicServer server;
    private final PubSubService pubSubService = PubSubService.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ChainHeadRpc chainHeadRpc;
    private final TransactionRpc transactionRpc;

    public WebSocketHandler(RPCMethods rpcMethods, ChainHeadRpc chainHeadRpc, TransactionRpc transactionRpc) {
        ObjectMapper mapper = new ObjectMapper();
        this.server = new JsonRpcBasicServer(mapper, rpcMethods);
        this.chainHeadRpc = chainHeadRpc;
        this.transactionRpc = transactionRpc;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        InputStream messageStream = new ByteArrayInputStream(message.asBytes());
        RpcRequest rpcRequest = mapper.readValue(messageStream, RpcRequest.class);
        log.log(Level.INFO, "SESSION ID: " + session.getId());
        log.log(Level.INFO, "METHOD: " + rpcRequest.getMethod());
        log.log(Level.INFO, "PARAMS: " + String.join(",", rpcRequest.getParams()));

        SubscriptionName method = SubscriptionName.fromString(rpcRequest.getMethod());
        switch (Objects.requireNonNull(method)) {
            case CHAIN_HEAD_UNSTABLE_FOLLOW -> {
                log.log(Level.INFO, "Subscribing for follow event");
                pubSubService.addSubscriber(Topic.UNSTABLE_FOLLOW, session);
                // This is temporary in order to simulate that our node "processes" blocks
                this.chainHeadRpc.chainUnstableFollow(Boolean.parseBoolean(rpcRequest.getParams()[0]));
            }
            case CHAIN_HEAD_UNSTABLE_UNFOLLOW -> {
                log.log(Level.INFO, "Unsubscribing from follow event");
                this.chainHeadRpc.chainUnstableUnfollow(rpcRequest.getParams()[0]);
                pubSubService.removeSubscriber(Topic.UNSTABLE_FOLLOW, session.getId());
            }
            case CHAIN_HEAD_UNSTABLE_UNPIN -> {
                log.log(Level.INFO, "Unpinning block");
                this.chainHeadRpc.chainUnstableUnpin(rpcRequest.getParams()[0], rpcRequest.getParams()[1]);
            }
            case CHAIN_HEAD_UNSTABLE_STORAGE -> {
                log.log(Level.INFO, "Querying storage");
                this.chainHeadRpc.chainUnstableStorage(rpcRequest.getParams()[0], rpcRequest.getParams()[1],
                        rpcRequest.getParams()[2]);
            }
            case CHAIN_HEAD_UNSTABLE_CALL -> {
                log.log(Level.INFO, "Executing unstable_call");
                this.chainHeadRpc.chainUnstableCall(rpcRequest.getParams()[0], rpcRequest.getParams()[1],
                        rpcRequest.getParams()[2], rpcRequest.getParams()[3]);
            }
            case CHAIN_HEAD_UNSTABLE_STOP_CALL -> {
                log.log(Level.INFO, "Executing unstable_stopCall");
                this.chainHeadRpc.chainUnstableStopCall(rpcRequest.getParams()[0]);
            }
            case TRANSACTION_UNSTABLE_SUBMIT_AND_WATCH -> {
                log.log(Level.INFO, "Executing submitAndWatch");
                pubSubService.addSubscriber(Topic.UNSTABLE_TRANSACTION_WATCH, session);
                this.transactionRpc.transactionUnstableSubmitAndWatch(rpcRequest.getParams()[0]);
            }
            case TRANSACTION_UNSTABLE_UNWATCH -> {
                log.log(Level.INFO, "Executing unstable_unwatch");
                this.transactionRpc.transactionUnstableUnwatch(rpcRequest.getParams()[0]);
                pubSubService.removeSubscriber(Topic.UNSTABLE_TRANSACTION_WATCH, session.getId());
            }
            default -> {
                // Server should only handle requests if it's not a sub/unsub request
                // Known issue: WS handler doesn't use interface method names (system_name)
                // instead it uses implementation ones (systemName)
                log.log(Level.INFO, "Handling the WS request using the normal RPC routes");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                server.handleRequest(messageStream, outputStream);
                session.sendMessage(new TextMessage(outputStream.toByteArray()));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // TODO: Search PubSubService for subscribers that have the closed session id
        // as a subscriber and remove it from the their subscription list
    }
}
