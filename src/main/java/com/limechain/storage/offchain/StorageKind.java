package com.limechain.storage.offchain;

import lombok.Getter;

public enum StorageKind {
    LOCAL("local_"), PERSISTENT("persistent_");

    @Getter
    private final String prefix;

    StorageKind(String prefix) {
        this.prefix = prefix;
    }
}
