package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.emeraldpay.polkaj.schnorrkel.SchnorrkelException;
import lombok.experimental.UtilityClass;
import org.web3j.crypto.MnemonicUtils;

@UtilityClass
public class Sr25519Utils {

    public static Schnorrkel.KeyPair generateKeyPair() {
        final Schnorrkel.KeyPair keyPair;
        try {
            keyPair = Schnorrkel.getInstance().generateKeyPair();
        } catch (SchnorrkelException e) {
            throw new RuntimeException(e);
        }

        return keyPair;
    }

    public static Schnorrkel.KeyPair generateKeyPair(final String mnemonic) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        byte[] secret = new byte[32];
        System.arraycopy(seed, 0, secret, 0, 32);
        final Schnorrkel.KeyPair rootKey;
        try {
            rootKey = Schnorrkel.getInstance().generateKeyPairFromSeed(secret);
        } catch (SchnorrkelException e) {
            throw new RuntimeException(e);
        }

        return rootKey;
    }

    public static byte[] signMessage(final byte[] publicKey, final byte[] privateKey, final byte[] message) {
        if (privateKey == null) return null;
        Schnorrkel.KeyPair keyPair = new Schnorrkel.KeyPair(publicKey, privateKey);
        try {
            return Schnorrkel.getInstance().sign(message, keyPair);
        } catch (SchnorrkelException e) {
            return null;
        }
    }

    public static boolean verifySignature(final VerifySignature signature) {
        try {
            Schnorrkel schnorrkel = Schnorrkel.getInstance();
            Schnorrkel.PublicKey publicKey = new Schnorrkel.PublicKey(signature.getPublicKeyData());
            return schnorrkel.verify(signature.getSignatureData(), signature.getMessageData(), publicKey);
        } catch (SchnorrkelException e) {
            //Todo: How to handle exceptions
            return false;
        }
    }

}
