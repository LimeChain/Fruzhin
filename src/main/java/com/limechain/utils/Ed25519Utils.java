package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Ed25519Kt;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.web3j.crypto.Hash.hmacSha512;

@UtilityClass
public class Ed25519Utils {

    public static Ed25519PrivateKey generateKeyPair() {
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(new SecureRandom());

        return new Ed25519PrivateKey(parameters);
    }

    public static Ed25519PrivateKey generateKeyPair(byte[] seed) {
        byte[] i = hmacSha512("ed25519 seed".getBytes(), seed);
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        Arrays.fill(i, (byte) 0);

        final Ed25519PrivateKeyParameters privateKeyParameters = new Ed25519PrivateKeyParameters(il, 0);
        return new Ed25519PrivateKey(privateKeyParameters);
    }

    public static Ed25519PrivateKey loadPrivateKey(final byte[] keyData) {
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(keyData);

        return new Ed25519PrivateKey(parameters);
    }

    public static byte[] signMessage(final byte[] privateKey, final byte[] message) {
        if (privateKey == null) return null;
        PrivKey privKey = Ed25519Kt.unmarshalEd25519PrivateKey(privateKey);
        return privKey.sign(message);
    }

    public static boolean verifySignature(final VerifySignature signature) {
        if (signature.getPublicKeyData() == null) return false;
        PubKey pubKey = Ed25519Kt.unmarshalEd25519PublicKey(signature.getPublicKeyData());
        return pubKey.verify(signature.getMessageData(), signature.getSignatureData());
    }
}
