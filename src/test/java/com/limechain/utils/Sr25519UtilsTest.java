package com.limechain.utils;

import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.MnemonicUtils;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sr25519UtilsTest {

    private final byte[] hashedMessage = HashUtils.hashWithBlake2b("This is test message".getBytes());

    @Test
    void generateSignVerify() {
        Schnorrkel.KeyPair keyPair = Sr25519Utils.generateKeyPair();

        byte[] signed = Sr25519Utils.signMessage(keyPair.getPublicKey(), keyPair.getSecretKey(), hashedMessage);

        VerifySignature signature = new VerifySignature(signed, hashedMessage, keyPair.getPublicKey(), Key.ED25519);
        boolean verified = Sr25519Utils.verifySignature(signature);

        assertTrue(verified);
    }

    @Test
    void generateKeyFromSeedNoException(){
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = secureRandom.generateSeed(16);
        String mnemonicString = MnemonicUtils.generateMnemonic(entropy);
        assertNotNull(mnemonicString);

        Schnorrkel.KeyPair privKey = Sr25519Utils.generateKeyPair(mnemonicString);

        assertNotNull(privKey);
    }

}