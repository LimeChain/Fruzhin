package com.limechain.chain.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.limechain.chain.spec.deserializers.TelemetryEndpointDeserializer;
import com.limechain.chain.spec.serializers.TelemetryEndpointSerializer;

import java.io.Serializable;

@JsonDeserialize(using = TelemetryEndpointDeserializer.class)
@JsonSerialize(using = TelemetryEndpointSerializer.class)
public record TelemetryEndpoint(String multiAddress, int verbosity) implements Serializable {
}
