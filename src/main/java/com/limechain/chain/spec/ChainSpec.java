package com.limechain.chain.spec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Contains the chain spec data, deserialized and parsed in-memory into appropriate structures
 */
@Getter
public class ChainSpec implements Serializable {
    private String name;
    private String id;
    private ChainType chainType;
    private String[] bootNodes;
    private TelemetryEndpoint[] telemetryEndpoints;
    private String protocolId;
    private Genesis genesis;
    private Map<String, PropertyValue> properties;
    private String[] forkBlocks;
    private String[] badBlocks;
    private String consensusEngine;
    private Map<String, String> lightSyncState;

    /**
     * Loads chain specification data from json file and maps its fields
     *
     * @param pathToChainSpecJSON path to the chain specification json file
     * @return class instance mapped to the json file
     * @throws IOException If path is invalid
     */
    public static ChainSpec newFromJSON(String pathToChainSpecJSON) throws IOException {
        final boolean failOnUnknownProperties = false;
        ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
        var file = new File(pathToChainSpecJSON);

        return objectMapper.readValue(file, ChainSpec.class);
    }
}
