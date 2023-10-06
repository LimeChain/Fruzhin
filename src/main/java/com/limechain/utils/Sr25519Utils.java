package com.limechain.utils;

import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.emeraldpay.polkaj.schnorrkel.SchnorrkelException;

public class Sr25519Utils {

    public static Schnorrkel.KeyPair generateKeyPair(){
        final Schnorrkel.KeyPair keyPair;
        try {
            keyPair = Schnorrkel.getInstance().generateKeyPair();
        } catch (SchnorrkelException e) {
            throw new RuntimeException(e);
        }

        return keyPair;
    }

    public static Schnorrkel.KeyPair generateKeyPair(byte[] seed) {
        final Schnorrkel.KeyPair rootKey;
        try {
            rootKey = Schnorrkel.getInstance().generateKeyPairFromSeed(seed);
        } catch (SchnorrkelException e) {
            throw new RuntimeException(e);
        }

        return rootKey;
    }

}
