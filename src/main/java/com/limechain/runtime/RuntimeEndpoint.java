package com.limechain.runtime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Holds all Runtime API endpoints' names.
 */
@Getter
@AllArgsConstructor
// TODO: Define all endpoints
public enum RuntimeEndpoint {
    CORE_EXECUTE_BLOCK("Core_execute_block"),
    CORE_VERSION("Core_version"),
    BLOCKBUILDER_CHECK_INHERENTS("BlockBuilder_check_inherents"),
    METADATA_METADATA("Metadata_metadata"),
    ;

    private final String name;
}
