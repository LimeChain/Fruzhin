package com.limechain.rpc.pubsub.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class JsonRpcWsErrorMessage {
    private final int id;
    private final String jsonrpc;
    private final int code;
    private final String message;
    private final String data;

    public JsonRpcWsErrorMessage(final int code, final String message, final String data) {
        this.id = 1;
        this.jsonrpc = "2.0";
        this.code = code;
        this.message = message;
        this.data = data;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{" +
                   "\"id\":" + id +
                   ",\"jsonrpc\":\"" + jsonrpc + '\"' +
                   ",\"code\":" + code +
                   (message == null ? "" : ",\"message\":\"" + message + '\"') +
                   (data == null ? "" : ",\"data:\"" + data + '\"') +
                   '}';
        }

    }
}
