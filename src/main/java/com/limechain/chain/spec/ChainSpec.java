package com.limechain.chain.spec;

import com.limechain.utils.json.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Contains the chain spec data, deserialized and parsed in-memory into appropriate structures
 */
@Getter
public class ChainSpec implements Serializable {
    private String id;
    private String name;
    private String protocolId;
    private String[] bootNodes;
    private Map<String, String> lightSyncState;

    /**
     * Loads chain specification data from json file and maps its fields
     *
     * @param pathToChainSpecJSON path to the chain specification json file
     * @return class instance mapped to the json file
     * @throws IOException If path is invalid
     */
    public static ChainSpec newFromJSON(String pathToChainSpecJSON) throws IOException {
        ObjectMapper mapper = new ObjectMapper(false);
        return mapper.mapToClass(pathToChainSpecJSON, ChainSpec.class);
    }
}
