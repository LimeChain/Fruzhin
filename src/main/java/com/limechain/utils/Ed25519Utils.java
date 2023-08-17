package com.limechain.utils;

import io.libp2p.crypto.keys.Ed25519PrivateKey;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

import java.security.SecureRandom;

@UtilityClass
public class Ed25519Utils {

    public static Ed25519PrivateKey generatePrivateKey(){
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(new SecureRandom());

        return new Ed25519PrivateKey(parameters);
    }

    public static Ed25519PrivateKey loadPrivateKey(final byte[] keyData){
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(keyData);

        return new Ed25519PrivateKey(parameters);
    }
}
