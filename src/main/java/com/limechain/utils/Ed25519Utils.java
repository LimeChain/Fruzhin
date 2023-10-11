package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Ed25519Kt;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

import java.security.SecureRandom;

@UtilityClass
public class Ed25519Utils {

    public static Ed25519PrivateKey generateKeyPair(){
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(new SecureRandom());

        return new Ed25519PrivateKey(parameters);
    }

    public static Ed25519PrivateKey generateKeyPair(byte[] seed) {
//        TODO: Implement it the correct way
        return new Ed25519PrivateKey(new Ed25519PrivateKeyParameters(new SecureRandom(seed)));
    }

    public static Ed25519PrivateKey loadPrivateKey(final byte[] keyData){
        final Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(keyData);

        return new Ed25519PrivateKey(parameters);
    }

    public static byte[] signMessage(final byte[] privateKey, final byte[] message){
        if(privateKey == null) return null;
        PrivKey privKey = Ed25519Kt.unmarshalEd25519PrivateKey(privateKey);
        return privKey.sign(message);
    }

    public static boolean verifySignature(final VerifySignature signature) {
        if(signature.getPublicKeyData() == null) return false;
        PubKey pubKey = Ed25519Kt.unmarshalEd25519PublicKey(signature.getPublicKeyData());
        return pubKey.verify(signature.getMessageData(), signature.getSignatureData());
    }
}
