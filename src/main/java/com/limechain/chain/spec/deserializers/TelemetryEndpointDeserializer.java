package com.limechain.chain.spec.deserializers;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.limechain.chain.spec.TelemetryEndpoint;

import java.io.IOException;

/**
 * Deserializes a {@link TelemetryEndpoint} from a JSON array (as in the JSON spec file).
 */
public class TelemetryEndpointDeserializer extends JsonDeserializer<TelemetryEndpoint> {

    @Override
    public TelemetryEndpoint deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (!node.isArray() || node.size() != 2) {
            throw new JsonMappingException(jsonParser, "Expected an array of two elements: (multiaddress, verbosity).");
        }

        String multiAddress = node.get(0).asText();
        int verbosity = node.get(1).asInt();
        return new TelemetryEndpoint(multiAddress, verbosity);
    }
}
