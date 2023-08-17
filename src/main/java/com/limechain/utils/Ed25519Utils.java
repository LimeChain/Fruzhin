package com.limechain.utils;

import io.libp2p.crypto.keys.Ed25519PrivateKey;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

import java.security.SecureRandom;

public class Ed25519Utils {
    private Ed25519Utils(){
        //Utils class
    }

    public static Ed25519PrivateKey generatePrivateKey(){
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(new SecureRandom());

        return new Ed25519PrivateKey(parameters);
    }

    public static Ed25519PrivateKey loadPrivateKey(final byte[] keyData){
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(keyData);

        return new Ed25519PrivateKey(parameters);
    }
}
