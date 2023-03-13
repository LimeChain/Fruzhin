package com.limechain.rpc.methods.chain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonResponse;
import com.googlecode.jsonrpc4j.JsonRpcResponseGenerator;
import com.limechain.rpc.methods.chain.events.FollowEvent;
import com.limechain.rpc.methods.chain.subscriptions.SubscriptionCalls;
import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;
import com.limechain.rpc.pubsub.Topic;
import com.limechain.rpc.pubsub.publisher.Publisher;
import com.limechain.rpc.pubsub.publisher.PublisherImpl;
import com.limechain.rpc.ws.client.WebSocketClient;
import io.emeraldpay.polkaj.api.Subscription;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Service
public class ChainRPCImpl {
    private final Publisher chainPublisher = new PublisherImpl();
    private final JsonRpcResponseGenerator resGen = new JsonRpcResponseGenerator();
    private final WebSocketClient wsClient;
    private final PubSubService pubSubService;

    public ChainRPCImpl(WebSocketClient wsClient, PubSubService pubSubService) {
        this.wsClient = wsClient;
        this.pubSubService = pubSubService;

        // This is temporary in order to simulate that our node processes blocks
        this.chainUnstableFollow(false);
    }

    public void chainUnstableFollow(boolean runtimeUpdates) {
        // TODO: Research how to retrieve subscription id from response
        Subscription<FollowEvent> subscription =
                this.wsClient.subscribeToEvent(SubscriptionCalls.unstableFollow(runtimeUpdates));

        subscription.handler((Subscription.Event<FollowEvent> event) -> {
            FollowEvent header = event.getResult();
            String response = classToJsonRPCResponse(header);
            chainPublisher.publish(new Message(Topic.UNSTABLE_FOLLOW.getValue(), response), pubSubService);
        });
    }

    //TODO: This shouldn't be in this class
    public String classToJsonRPCResponse(Object classToConvert) {
        // TODO: Extend resGen.createResponse to be able to include params.subscription field if available
        JsonResponse response = resGen.createResponse("2.0", "1",
                new ObjectMapper()
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL).
                        valueToTree(classToConvert),
                null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writeAndFlushValue(out, response.getResponse());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toString();
    }

    private void writeAndFlushValue(OutputStream output, JsonNode value) throws IOException {
        if (value == null) {
            return;
        }

        new ObjectMapper().writeValue(output, value);
        output.write('\n');
    }


    public String chainUnstableUnfollow(String sessionId) {
        pubSubService.removeSubscriber(Topic.UNSTABLE_FOLLOW, sessionId);
        return sessionId;
    }

    public String chainUnstableUnpin() {
        return null;
    }

    public String chainUnstableStorage() {
        return null;
    }

    public String chainUnstableCall() {
        return null;
    }

    public String chainUnstableStopCall() {
        return null;
    }
}
