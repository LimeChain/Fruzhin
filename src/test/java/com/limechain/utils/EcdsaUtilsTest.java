package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import kotlin.Pair;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.MnemonicUtils;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcdsaUtilsTest {

    private final byte[] hashedMessage = HashUtils.hashWithBlake2b("This is test message".getBytes());

    @Test
    void generateSignVerify() {
        Pair<PrivKey, PubKey> keyPair = EcdsaUtils.generateKeyPair();
        byte[] signed = EcdsaUtils.signMessage(keyPair.getFirst().raw(), hashedMessage);

        VerifySignature signature = new VerifySignature(signed, hashedMessage, keyPair.getSecond().raw(), Key.ECDSA);
        boolean verified = EcdsaUtils.verifySignature(signature);

        assertTrue(verified);
    }

    @Test
    void generateKeyFromSeedNoException() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = secureRandom.generateSeed(16);
        String mnemonicString = MnemonicUtils.generateMnemonic(entropy);
        assertNotNull(mnemonicString);

        Pair<PrivKey, PubKey> keyPair = EcdsaUtils.generateKeyPair(mnemonicString);

        assertNotNull(keyPair);
        assertNotNull(keyPair.getFirst());
    }

}