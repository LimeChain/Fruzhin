package com.limechain.rpc.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RpcResponse {
    public static final String JSONRPC = "jsonrpc";
    public static final String ID = "id";
    public static final String RESULT = "result";
    public static final String METHOD = "method";
    public static final String PARAMS = "params";
    public static final String SUBSCRIPTION = "subscription";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String createForSubscription(String method, JsonNode result, String jsonRpc) {
        ObjectNode response = MAPPER.createObjectNode();
        response.put(JSONRPC, jsonRpc);
        response.put(METHOD, method);

        ObjectNode params = MAPPER.createObjectNode();
        params.put(SUBSCRIPTION, "123");
        params.put(RESULT, result);

        response.put(PARAMS, params);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            writeAndFlushValue(out, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toString();
    }

    private static void writeAndFlushValue(OutputStream output, JsonNode value) throws IOException {
        if (value == null) {
            return;
        }

        MAPPER.writeValue(output, value);
        output.write("%n".getBytes());
    }
    
}
