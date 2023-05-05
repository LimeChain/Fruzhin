package com.limechain.chain.lightsyncstate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LightSyncStateTest {
    private static LightSyncState execute_decoding(String pathname) throws IOException {
        JsonNode root = new ObjectMapper().readTree(new File(pathname));
        LightSyncState decoded = LightSyncState.decode(new HashMap<>() {{
            put("finalizedBlockHeader", root.path("lightSyncState").get("finalizedBlockHeader").textValue());
            put("babeEpochChanges", root.path("lightSyncState").get("babeEpochChanges").textValue());
            put("grandpaAuthoritySet", root.path("lightSyncState").get("grandpaAuthoritySet").textValue());
        }});

        return decoded;
    }

    @Test
    void decode_polkadot() throws IOException {
        LightSyncState decoded = execute_decoding("genesis/polkadot.json");
        assertNotNull(decoded);
    }

    @Test
    void decode_kusama() throws IOException {
        LightSyncState decoded = execute_decoding("genesis/kusama.json");
        assertNotNull(decoded);
    }

    @Test
    void decode_westend() throws IOException {
        LightSyncState decoded = execute_decoding("genesis/westend.json");
        assertNotNull(decoded);
    }
}