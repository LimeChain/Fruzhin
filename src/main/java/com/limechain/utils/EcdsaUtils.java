package com.limechain.utils;

import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.EcdsaKt;
import io.libp2p.crypto.keys.EcdsaPrivateKey;
import io.libp2p.crypto.keys.EcdsaPublicKey;
import kotlin.Pair;

public class EcdsaUtils {

    public static Pair<EcdsaPrivateKey, EcdsaPublicKey> generateKeyPair() {
        Pair<PrivKey, PubKey> keyPair = EcdsaKt.generateEcdsaKeyPair();
        return new Pair<>((EcdsaPrivateKey) keyPair.getFirst(), (EcdsaPublicKey) keyPair.getSecond());
    }

    public static Pair<EcdsaPrivateKey, EcdsaPublicKey> generateKeyPair(String seed) {
        return EcdsaKt.generateEcdsaKeyPair(seed);
    }
}
