package com.limechain.utils;

import io.libp2p.core.crypto.PrivKey;
import io.libp2p.crypto.keys.Ed25519Kt;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

import java.security.SecureRandom;

@UtilityClass
public class Ed25519Utils {

    /**
     * Generates Ed25119 key pair
     * @return Ed25519 Private key (32 bytes) and Public key (32 bytes) which is attached to the private key
     */
    public static Ed25519PrivateKey generateKeyPair() {
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(new SecureRandom());

        return new Ed25519PrivateKey(parameters);
    }

    /**
     * Loads Ed25519 keypair from byte array
     * @param keyData 32 bytes Ed25519 private key
     * @return Ed25519 Private key (32 bytes) and Public key (32 bytes) which is attached to the private key
     */
    public static Ed25519PrivateKey loadPrivateKey(final byte[] keyData) {
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(keyData);

        return new Ed25519PrivateKey(parameters);
    }

    /**
     * Signs message with Ed25519 private key
     * @param privateKey 32 bytes Ed25519 private key
     * @param message message to be signed
     * @return 64 bytes signature
     */
    public static byte[] signMessage(final byte[] privateKey, final byte[] message) {
        if (privateKey == null) return null;
        PrivKey privKey = Ed25519Kt.unmarshalEd25519PrivateKey(privateKey);
        return privKey.sign(message);
    }
}
