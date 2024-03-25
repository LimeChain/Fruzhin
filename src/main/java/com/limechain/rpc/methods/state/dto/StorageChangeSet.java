package com.limechain.rpc.methods.state.dto;

import lombok.Data;

import java.util.Map;

@Data
public class StorageChangeSet {
    private final String block;
    private final Map<String, String> changes;
}
