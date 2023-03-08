package com.limechain.chain;

import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@Log
public class ChainSpecTest {

    @Test
    public void parseJsonToChainSpec() {
        String pathToPolkadotTestJSON = "./src/test/resources/short_polkadot.json";
        try {
            ChainSpec chainSpec = ChainSpec.newFromJSON(pathToPolkadotTestJSON);
            String actualName = chainSpec.getName();
            String expectedName = "Polkadot";
            assertEquals(expectedName, actualName);

            String actualId = chainSpec.getId();
            String expectedId = "polkadot";
            assertEquals(expectedId, actualId);

            String actualChainType = chainSpec.getChainType();
            String expectedChainType = "Live";
            assertEquals(expectedChainType, actualChainType);

            String actualProtocolId = chainSpec.getProtocolId();
            String expectedProtocolId = "dot";
            assertEquals(expectedProtocolId, actualProtocolId);

            String[] actualForkBlocks = chainSpec.getForkBlocks();
            assertNull(actualForkBlocks);

            String[] actualBadBlocks = chainSpec.getBadBlocks();
            assertNull(actualBadBlocks);

            String actualConsensusEngine = chainSpec.getConsensusEngine();
            assertNull(actualConsensusEngine);

            String[] actualBootNodes = chainSpec.getBootNodes();
            String[] expectedBootNodes = new String[]{
                    "/dns/polkadot-connect-0.parity.io/tcp/443/wss/p2p/12D3KooWEPmjoRpDSUuiTjvyNDd8fejZ9eNWH5bE965nyBMDrB4o",
                    "/dns/cc1-1.parity.tech/tcp/30333/p2p/12D3KooWFN2mhgpkJsDBuNuE5427AcDrsib8EoqGMZmkxWwx3Md4"};
            assertArrayEquals(expectedBootNodes, actualBootNodes);

            Object[] actualTelemetryEndpoints = chainSpec.getTelemetryEndpoints();
            ArrayList<Object> expectedTelemetryEndpoints = new ArrayList<>();
            expectedTelemetryEndpoints.add("wss://telemetry.polkadot.io/submit/");
            expectedTelemetryEndpoints.add(
                    Integer.valueOf(0));
            assertEquals(expectedTelemetryEndpoints, actualTelemetryEndpoints[0]);

            Map actualTopValue = chainSpec.getGenesis().getRaw().get("top");
            Map expectedTopValue = new LinkedHashMap();
            expectedTopValue.put("0x9c5d795d0297be56027a4b2464e3339763e6d3c1fb15805edfd024172ea4817d9e40ca7bd1fd588ca534ee6b96a65ca8a53ec232dda838cc3cd2bd1887904906", "0x11bc2c7ea454e083cea1186239abc83733200e78");
            expectedTopValue.put("0x9c5d795d0297be56027a4b2464e333979c5d795d0297be56027a4b2464e33397eb0718ce75762eeba4570943d5b2de2afb9085b6", "0x000e760ff72301000000000000000000");
            assertEquals(expectedTopValue, actualTopValue);

            Map actualChildrenDefault = chainSpec.getGenesis().getRaw().get("childrenDefault");
            Map expectedChildrenDefault = new LinkedHashMap();
            assertEquals(expectedChildrenDefault, actualChildrenDefault);

            Map actualRuntime = chainSpec.getGenesis().getRuntime();
            assertNull(actualRuntime);

            Map actualProperties = chainSpec.getProperties();
            Map expectedProperties = new LinkedHashMap();
            expectedProperties.put("ss58Format", 0);
            expectedProperties.put("tokenDecimals", 10);
            expectedProperties.put("tokenSymbol", "DOT");

            assertEquals(expectedProperties, actualProperties);

            System.out.println(chainSpec);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error loading chain spec from json", e);
            fail();
        }
    }
}
