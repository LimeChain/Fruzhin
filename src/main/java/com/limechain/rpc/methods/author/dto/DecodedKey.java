package com.limechain.rpc.methods.author.dto;

import com.limechain.storage.crypto.KeyType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DecodedKey {
    private byte[] data;
    private KeyType keyType;
}