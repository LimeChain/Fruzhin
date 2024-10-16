package com.limechain.rpc.methods.author.dto;

import com.limechain.storage.crypto.KeyType;
import lombok.Value;

@Value
public class DecodedKey {
    byte[] data;
    KeyType keyType;
}