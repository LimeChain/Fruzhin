package com.limechain.chain.spec.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.limechain.chain.spec.TelemetryEndpoint;

import java.io.IOException;

/**
 * Serializes a {@link TelemetryEndpoint} to a JSON array (as in the JSON spec file).
 */
public class TelemetryEndpointSerializer extends JsonSerializer<TelemetryEndpoint> {

    @Override
    public void serialize(TelemetryEndpoint telemetryEndpoint, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        gen.writeStartArray();
        gen.writeString(telemetryEndpoint.multiAddress());
        gen.writeNumber(telemetryEndpoint.verbosity());
        gen.writeEndArray();
    }
}
