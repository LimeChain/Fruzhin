package com.limechain.utils;

import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.MnemonicUtils;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class Sr25519UtilsTest {

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