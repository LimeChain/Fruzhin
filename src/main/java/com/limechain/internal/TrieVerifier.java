package com.limechain.internal;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UByteReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
public class TrieVerifier {

    public static final int MAX_PARTIAL_KEY_LENGTH = 65535;

    public boolean verify(byte[][] encodedProofNodes, Hash256 rootHash, byte[] value) {

        return true;
    }

    public static Node buildTrie(byte[][] encodedProofNodes, byte[] rootHash) {
        if (encodedProofNodes.length == 0) {
            throw new IllegalStateException("Encoded proof nodes is empty!");
        }

        Map<String, byte[]> digestToEncoding = new HashMap<>(encodedProofNodes.length);

        Node root = new Node();

        for (byte[] encodedProofNode : encodedProofNodes) {

        }
        return new Node();
    }


}

