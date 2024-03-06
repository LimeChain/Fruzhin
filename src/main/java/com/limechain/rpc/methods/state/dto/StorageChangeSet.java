package com.limechain.rpc.methods.state.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class StorageChangeSet {
    private String block;
    private Map<String, String> changes;
}
