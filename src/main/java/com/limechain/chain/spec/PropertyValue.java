package com.limechain.chain.spec;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * A marker class to simply combine the semantics of
 * {@link JsonNode} as an unparsed json node (ready to be serialized again as-is)
 * with the {@link Serializable} marker interface needed for consistent persistence to database.
 * <br>
 * Since the value of the properties in the chain spec is arbitrary and never interpreted by the local node,
 * but will only be served to RPC callers as-is, we store the data as JsonNode
 * that is ready to be easily serialized to JSON again.
 */
public abstract class PropertyValue extends JsonNode implements Serializable {
}
