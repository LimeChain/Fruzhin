package com.limechain.rpc.pubsub.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class JsonRpcWsResponseMessage {
    private final String jsonrpc;
    private final String method;
    private final JsonRpcResponseResult params;

    public JsonRpcWsResponseMessage(final String message, final String method) {
        this.params = new JsonRpcResponseResult(message);
        this.jsonrpc = "2.0";
        this.method = method;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{" +
                   ",\"jsonrpc\":\"" + jsonrpc + '\"' +
                   ",\"method\":" + method +
                   ",\"params\":" + "{ \"result\": " + params.getResult() + " }" +
                   '}';
        }

    }
}

