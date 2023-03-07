package com.limechain.chain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class ChainSpec implements Serializable {
    public String name;
    public String id;
    public String chainType;
    public String[] bootNodes;
    public Object[] telemetryEndpoints;
    public String protocolId;
    public Fields genesis;
    public Map<String, Object> properties;
    public String[] forkBlocks;
    public String[] badBlocks;
    public String consensusEngine;

    public static ChainSpec newFromJSON(String pathToChainSpecJSON) throws IOException {
        final boolean failOnUnknownProperties = false;
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
        var file = new File(pathToChainSpecJSON);

        ChainSpec gen = objectMapper.readValue(file, ChainSpec.class);
        return gen;
    }

    public class Fields implements Serializable {
        public Map<String, Map<String, String>> raw;
        public Map<String, Map<String, Object>> runtime;
    }
}

