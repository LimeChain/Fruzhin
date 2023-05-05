package com.limechain.chain.lightsyncstate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LightSyncStateTest {
    private static LightSyncState executeDecoding(String pathname) throws IOException {
        JsonNode root = new ObjectMapper().readTree(new File(pathname));
        LightSyncState decoded = LightSyncState.decode(new HashMap<>() {{
            put("finalizedBlockHeader", root.path("lightSyncState").get("finalizedBlockHeader").textValue());
            put("babeEpochChanges", root.path("lightSyncState").get("babeEpochChanges").textValue());
            put("grandpaAuthoritySet", root.path("lightSyncState").get("grandpaAuthoritySet").textValue());
        }});

        return decoded;
    }

    @Test
    void decodePolkadot() throws IOException {
        LightSyncState decoded = executeDecoding("genesis/polkadot.json");
        assertNotNull(decoded);
    }

    @Test
    void decodeKusama() throws IOException {
        LightSyncState decoded = executeDecoding("genesis/kusama.json");
        assertNotNull(decoded);
    }

    @Test
    void decodeWestend() throws IOException {
        LightSyncState decoded = executeDecoding("genesis/westend.json");
        assertNotNull(decoded);
    }
}