package com.limechain.rpc.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

public class RpcResponse {
    public static final String JSONRPC = "jsonrpc";
    public static final String ID = "id";
    public static final String RESULT = "result";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static void writeAndFlushValue(OutputStream output, JsonNode value) throws IOException {
        if (value == null) {
            return;
        }

        new ObjectMapper().writeValue(output, value);
        output.write('\n');
    }

    public static String create(String jsonRpc, Object id, JsonNode result) {
        ObjectNode response = MAPPER.createObjectNode();
        response.put(JSONRPC, jsonRpc);
        if (id instanceof Integer) {
            response.put(ID, ((Integer) id).intValue());
        } else if (id instanceof Long) {
            response.put(ID, ((Long) id).longValue());
        } else if (id instanceof Float) {
            response.put(ID, ((Float) id).floatValue());
        } else if (id instanceof Double) {
            response.put(ID, ((Double) id).doubleValue());
        } else if (id instanceof BigDecimal) {
            response.put(ID, (BigDecimal) id);
        } else {
            response.put(ID, (String) id);
        }

        response.set(RESULT, result);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            writeAndFlushValue(out, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toString();
    }


}
