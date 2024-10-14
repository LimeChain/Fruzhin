package com.limechain.runtime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Used to identify runtime API endpoints by their names as listed in the spec.
 *
 * @see <a href="https://spec.polkadot.network/chap-runtime-api">the spec</a>
 */
// NOTE: Add here whatever endpoints necessary during development.
@Getter
@AllArgsConstructor
public enum RuntimeEndpoint {
    CORE_EXECUTE_BLOCK("Core_execute_block"),
    CORE_VERSION("Core_version"),
    BLOCKBUILDER_CHECK_INHERENTS("BlockBuilder_check_inherents"),
    METADATA_METADATA("Metadata_metadata"),
    TRANSACTION_QUEUE_VALIDATE_TRANSACTION("TaggedTransactionQueue_validate_transaction"),
    ;

    private final String name;
}
