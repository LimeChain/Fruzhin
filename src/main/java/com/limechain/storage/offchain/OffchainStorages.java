package com.limechain.storage.offchain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A container for all three different "storages", necessary for the offchain host API.
 */
@Getter
@AllArgsConstructor
public class OffchainStorages {
    BasicStorage localStorage;
    BasicStorage persistentStorage;
    BasicStorage baseStorage; // for offchain_index host APIs
}
