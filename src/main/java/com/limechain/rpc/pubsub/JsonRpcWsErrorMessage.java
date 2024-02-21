package com.limechain.rpc.pubsub;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@Getter
public class JsonRpcWsErrorMessage {
    private final int id = 1;
    private final String jsonrpc = "2.0";
    private final int code;
    private final String message;
    private final String data;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{" +
                   "\"id\":" + id +
                   ",5\"jsonrpc\":\"" + jsonrpc + '\"' +
                   ",\"code\":" + code +
                   (message == null ? "" : ",\"message\"=\"" + message + '\"') +
                   (data == null ? "" : ",\"data=\"" + data + '\"') +
                   '}';
        }

    }
}
