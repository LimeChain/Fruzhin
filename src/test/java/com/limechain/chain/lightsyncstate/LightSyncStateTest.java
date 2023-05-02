package com.limechain.chain.lightsyncstate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LightSyncStateTest {
    @Test
    void decode_polkadot() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(new File("genesis/polkadot.json"));
        LightSyncState decoded = LightSyncState.decode(new HashMap<>() {{
            put("finalizedBlockHeader", root.path("lightSyncState").get("finalizedBlockHeader").textValue());
            put("babeEpochChanges", root.path("lightSyncState").get("babeEpochChanges").textValue());
            put("grandpaAuthoritySet", root.path("lightSyncState").get("grandpaAuthoritySet").textValue());
        }});

        assertNotNull(decoded);
    }

    @Test
    void decode_kusama() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(new File("genesis/kusama.json"));
        LightSyncState decoded = LightSyncState.decode(new HashMap<>() {{
            put("finalizedBlockHeader", root.path("lightSyncState").get("finalizedBlockHeader").textValue());
            put("babeEpochChanges", root.path("lightSyncState").get("babeEpochChanges").textValue());
            put("grandpaAuthoritySet", root.path("lightSyncState").get("grandpaAuthoritySet").textValue());
        }});

        assertNotNull(decoded);
    }

    @Test
    void decode_westend() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(new File("genesis/westend.json"));
        LightSyncState decoded = LightSyncState.decode(new HashMap<>() {{
            put("finalizedBlockHeader", root.path("lightSyncState").get("finalizedBlockHeader").textValue());
            put("babeEpochChanges", root.path("lightSyncState").get("babeEpochChanges").textValue());
            put("grandpaAuthoritySet", root.path("lightSyncState").get("grandpaAuthoritySet").textValue());
        }});

        assertNotNull(decoded);

    }
}