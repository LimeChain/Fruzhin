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
    void decodeNetworks() throws IOException {
        var polkadot = executeDecoding("genesis/polkadot.json");
        var kusama = executeDecoding("genesis/ksmcc3.json");
        var westend = executeDecoding("genesis/westend2.json");

        assertNotNull(polkadot);
        assertNotNull(kusama);
        assertNotNull(westend);
    }

    private static LightSyncState executeDecoding(String pathname) throws IOException {
        JsonNode root = new ObjectMapper().readTree(new File(pathname));
        LightSyncState decoded = LightSyncState.decode(new HashMap<>() {{
            put("finalizedBlockHeader", root.path("lightSyncState").get("finalizedBlockHeader").textValue());
            put("babeEpochChanges", root.path("lightSyncState").get("babeEpochChanges").textValue());
            put("grandpaAuthoritySet", root.path("lightSyncState").get("grandpaAuthoritySet").textValue());
        }});

        return decoded;
    }
}