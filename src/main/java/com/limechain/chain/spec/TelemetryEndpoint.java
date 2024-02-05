package com.limechain.chain.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.limechain.chain.spec.deserializers.TelemetryEndpointDeserializer;
import com.limechain.chain.spec.serializers.TelemetryEndpointSerializer;

import java.io.Serializable;

/**
 * Refer to <a href="https://spec.polkadot.network/id-cryptography-encoding#section-chainspec">the spec</a>.
 * @param multiAddress the multiaddress of the endpoint
 * @param verbosity an int from 0 to 9 with 0 indicating the lowest verbosity
 */
@JsonDeserialize(using = TelemetryEndpointDeserializer.class)
@JsonSerialize(using = TelemetryEndpointSerializer.class)
public record TelemetryEndpoint(String multiAddress, int verbosity) implements Serializable {
}
