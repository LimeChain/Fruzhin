package com.limechain.runtime.hostapi;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import com.limechain.utils.EcdsaUtils;
import com.limechain.utils.Ed25519Utils;
import com.limechain.utils.HashUtils;
import com.limechain.utils.Sr25519Utils;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import kotlin.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoHostFunctionsTest {

    private static Ed25519PrivateKey ed25519PrivateKey;
    private static Schnorrkel.KeyPair sr25519KeyPair;
    private static Pair<PrivKey, PubKey> ecdsaKeyPair;
    String seed = "enhance panic apple roof clinic wrist trigger foster denial imitate resource grain";
    //Existing seed is 1 and seed bytes
    byte[] existingSeed = {1, 73, 1, 101, 110, 104, 97, 110, 99, 101, 32, 112, 97, 110, 105, 99, 32, 97, 112, 112, 108,
            101, 32, 114, 111, 111, 102, 32, 99, 108, 105, 110, 105, 99, 32, 119, 114, 105, 115, 116, 32, 116, 114, 105,
            103, 103, 101, 114, 32, 102, 111, 115, 116, 101, 114, 32, 100, 101, 110, 105, 97, 108, 32, 105, 109, 105,
            116, 97, 116, 101, 32, 114, 101, 115, 111, 117, 114, 99, 101, 32, 103, 114, 97, 105, 110};
    byte[] missingSeed = {0};
    byte[] message = "Test message".getBytes();
    byte[] hashedMessage = HashUtils.hashWithBlake2b(message);
    int keyPosition = 123;
    int publicKeyPosition = 456;
    int signaturePosition = 789;
    int returnPosition = 999;
    @InjectMocks
    private CryptoHostFunctions cryptoHostFunctions;
    @Spy
    private Set<VerifySignature> signaturesToVerify = new HashSet<>();
    @Mock
    private HostApi hostApi;
    @Mock
    private RuntimePointerSize seedPointer;
    @Mock
    private RuntimePointerSize messagePointer;
    @Mock
    private RuntimePointerSize returnPointer;
    @Mock
    private KeyStore keyStore;

    @BeforeAll
    static void beforeAll() {
        ed25519PrivateKey = Ed25519Utils.generateKeyPair();
        sr25519KeyPair = Sr25519Utils.generateKeyPair();
        ecdsaKeyPair = EcdsaUtils.generateKeyPair();
    }

    @Test
    void ed25519PublicKeysV1() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.GRANDPA.getBytes());
        when(hostApi.addDataToMemory(any())).thenReturn(returnPointer);

        RuntimePointerSize result = cryptoHostFunctions.ed25519PublicKeysV1(keyPosition);

        verify(keyStore).getPublicKeysByKeyType(KeyType.GRANDPA);
        assertEquals(returnPointer, result);
    }

    @Test
    void ed25519GenerateV1_no_seed() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.GRANDPA.getBytes());
        when(hostApi.getDataFromMemory(seedPointer)).thenReturn(missingSeed);
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ed25519Statick = mockStatic(Ed25519Utils.class)) {
            ed25519Statick.when(Ed25519Utils::generateKeyPair).thenReturn(ed25519PrivateKey);

            int result = cryptoHostFunctions.ed25519GenerateV1(keyPosition, seedPointer);

            ed25519Statick.verify(Ed25519Utils::generateKeyPair);
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void ed25519GenerateV1_with_seed() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.GRANDPA.getBytes());
        when(hostApi.getDataFromMemory(seedPointer)).thenReturn(existingSeed);
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ed25519Statick = mockStatic(Ed25519Utils.class)) {
            ed25519Statick.when(() -> Ed25519Utils.generateKeyPair(seed)).thenReturn(ed25519PrivateKey);

            int result = cryptoHostFunctions.ed25519GenerateV1(keyPosition, seedPointer);

            ed25519Statick.verify(() -> Ed25519Utils.generateKeyPair(seed));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void ed25519SignV1() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.GRANDPA.getBytes());
        when(hostApi.getDataFromMemory(publicKeyPosition, 32)).thenReturn(ed25519PrivateKey.publicKey().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        when(keyStore.get(KeyType.GRANDPA, ed25519PrivateKey.publicKey().raw())).thenReturn(ed25519PrivateKey.raw());
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ed25519Statick = mockStatic(Ed25519Utils.class)) {
            long result = cryptoHostFunctions.ed25519SignV1(keyPosition, publicKeyPosition, messagePointer);

            ed25519Statick.verify(() -> Ed25519Utils.signMessage(ed25519PrivateKey.raw(), message));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void ed25519VerifyV1() {
        when(hostApi.getDataFromMemory(publicKeyPosition, 32)).thenReturn(ed25519PrivateKey.publicKey().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(ed25519PrivateKey.sign(message));

        int verify = cryptoHostFunctions.ed25519VerifyV1(signaturePosition, messagePointer, publicKeyPosition);
        assertEquals(1, verify);
    }

    @Test
    void ed25519BatchVerifyV1_with_verification_off() {
        when(hostApi.getDataFromMemory(publicKeyPosition, 32)).thenReturn(ed25519PrivateKey.publicKey().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(ed25519PrivateKey.sign(message));

        assertFalse(cryptoHostFunctions.batchVerificationStarted);

        int verify = cryptoHostFunctions.ed25519BatchVerifyV1(signaturePosition, messagePointer, publicKeyPosition);

        verifyNoInteractions(signaturesToVerify);
        assertEquals(1, verify);
    }

    @Test
    void ed25519BatchVerifyV1_with_verification_on() {
        cryptoHostFunctions.batchVerificationStarted = true;
        when(hostApi.getDataFromMemory(publicKeyPosition, 32)).thenReturn(ed25519PrivateKey.publicKey().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(ed25519PrivateKey.sign(message));

        assertTrue(cryptoHostFunctions.batchVerificationStarted);

        int verify = cryptoHostFunctions.ed25519BatchVerifyV1(signaturePosition, messagePointer, publicKeyPosition);

        verify(signaturesToVerify).add(any());
        assertEquals(1, verify);
    }

    @Test
    void sr25519PublicKeysV1() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.BABE.getBytes());
        when(hostApi.addDataToMemory(any())).thenReturn(returnPointer);

        RuntimePointerSize result = cryptoHostFunctions.sr25519PublicKeysV1(keyPosition);

        verify(keyStore).getPublicKeysByKeyType(KeyType.BABE);
        assertEquals(returnPointer, result);
    }

    @Test
    void sr25519GenerateV1_no_seed() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.BABE.getBytes());
        when(hostApi.getDataFromMemory(seedPointer)).thenReturn(missingSeed);
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var sr25519Statick = mockStatic(Sr25519Utils.class)) {
            sr25519Statick.when(Sr25519Utils::generateKeyPair).thenReturn(sr25519KeyPair);

            int result = cryptoHostFunctions.sr25519GenerateV1(keyPosition, seedPointer);

            sr25519Statick.verify(Sr25519Utils::generateKeyPair);
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void sr25519GenerateV1_with_seed() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.BABE.getBytes());
        when(hostApi.getDataFromMemory(seedPointer)).thenReturn(existingSeed);
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var sr25519Statick = mockStatic(Sr25519Utils.class)) {
            sr25519Statick.when(() -> Sr25519Utils.generateKeyPair(seed)).thenReturn(sr25519KeyPair);

            int result = cryptoHostFunctions.sr25519GenerateV1(keyPosition, seedPointer);

            sr25519Statick.verify(() -> Sr25519Utils.generateKeyPair(seed));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void sr25519SignV1() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.BABE.getBytes());
        when(hostApi.getDataFromMemory(publicKeyPosition, 32)).thenReturn(sr25519KeyPair.getPublicKey());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        when(keyStore.get(KeyType.BABE, sr25519KeyPair.getPublicKey())).thenReturn(sr25519KeyPair.getSecretKey());
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var sr25519Statick = mockStatic(Sr25519Utils.class)) {
            int result = cryptoHostFunctions.sr25519SignV1(keyPosition, publicKeyPosition, messagePointer);
            sr25519Statick.verify(() ->
                    Sr25519Utils.signMessage(sr25519KeyPair.getPublicKey(), sr25519KeyPair.getSecretKey(), message));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void sr25519VerifyV1() {
        when(hostApi.getDataFromMemory(publicKeyPosition, 32)).thenReturn(sr25519KeyPair.getPublicKey());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        byte[] sig = Sr25519Utils.signMessage(sr25519KeyPair.getPublicKey(), sr25519KeyPair.getSecretKey(), message);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(sig);

        int verify = cryptoHostFunctions.sr25519VerifyV1(signaturePosition, messagePointer, publicKeyPosition);
        assertEquals(1, verify);
    }

    @Test
    void sr25519BatchVerifyV1_with_verification_off() {
        when(hostApi.getDataFromMemory(publicKeyPosition, 32)).thenReturn(sr25519KeyPair.getPublicKey());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        byte[] sig = Sr25519Utils.signMessage(sr25519KeyPair.getPublicKey(), sr25519KeyPair.getSecretKey(), message);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(sig);

        assertFalse(cryptoHostFunctions.batchVerificationStarted);

        int verify = cryptoHostFunctions.sr25519BatchVerifyV1(signaturePosition, messagePointer, publicKeyPosition);

        verifyNoInteractions(signaturesToVerify);
        assertEquals(1, verify);
    }

    @Test
    void sr25519BatchVerifyV1_with_verification_on() {
        cryptoHostFunctions.batchVerificationStarted = true;
        when(hostApi.getDataFromMemory(publicKeyPosition, 32)).thenReturn(sr25519KeyPair.getPublicKey());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        byte[] sig = Sr25519Utils.signMessage(sr25519KeyPair.getPublicKey(), sr25519KeyPair.getSecretKey(), message);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(sig);

        assertTrue(cryptoHostFunctions.batchVerificationStarted);

        int verify = cryptoHostFunctions.sr25519BatchVerifyV1(signaturePosition, messagePointer, publicKeyPosition);

        verify(signaturesToVerify).add(any());
        assertEquals(1, verify);
    }

    @Test
    void ecdsaPublicKeysV1() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.CONTROLLING_ACCOUNTS.getBytes());
        when(hostApi.addDataToMemory(any())).thenReturn(returnPointer);

        RuntimePointerSize result = cryptoHostFunctions.sr25519PublicKeysV1(keyPosition);

        verify(keyStore).getPublicKeysByKeyType(KeyType.CONTROLLING_ACCOUNTS);
        assertEquals(returnPointer, result);
    }

    @Test
    void ecdsaGenerateV1_no_seed() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.CONTROLLING_ACCOUNTS.getBytes());
        when(hostApi.getDataFromMemory(seedPointer)).thenReturn(missingSeed);
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ecdsaUtil = mockStatic(EcdsaUtils.class)) {
            ecdsaUtil.when(EcdsaUtils::generateKeyPair).thenReturn(ecdsaKeyPair);

            int result = cryptoHostFunctions.ecdsaGenerateV1(keyPosition, seedPointer);

            ecdsaUtil.verify(EcdsaUtils::generateKeyPair);
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void ecdsaGenerateV1_use_seed() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.CONTROLLING_ACCOUNTS.getBytes());
        when(hostApi.getDataFromMemory(seedPointer)).thenReturn(existingSeed);
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ecdsaUtil = mockStatic(EcdsaUtils.class)) {
            ecdsaUtil.when(() -> EcdsaUtils.generateKeyPair(seed)).thenReturn(ecdsaKeyPair);

            int result = cryptoHostFunctions.ecdsaGenerateV1(keyPosition, seedPointer);

            ecdsaUtil.verify(() -> EcdsaUtils.generateKeyPair(seed));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void ecdsaSignV1() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.CONTROLLING_ACCOUNTS.getBytes());
        when(hostApi.getDataFromMemory(publicKeyPosition, 33)).thenReturn(ecdsaKeyPair.getSecond().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        when(keyStore.get(KeyType.CONTROLLING_ACCOUNTS, ecdsaKeyPair.getSecond().raw()))
                .thenReturn(ecdsaKeyPair.getFirst().raw());
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ecdsaStatic = mockStatic(EcdsaUtils.class)) {
            int result = cryptoHostFunctions.ecdsaSignV1(keyPosition, publicKeyPosition, messagePointer);

            ecdsaStatic.verify(() ->
                    EcdsaUtils.signMessage(ecdsaKeyPair.getFirst().raw(), hashedMessage));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void ecdsaSignPrehashedV1() {
        when(hostApi.getDataFromMemory(keyPosition, 4)).thenReturn(KeyType.CONTROLLING_ACCOUNTS.getBytes());
        when(hostApi.getDataFromMemory(publicKeyPosition, 33)).thenReturn(ecdsaKeyPair.getSecond().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        when(keyStore.get(KeyType.CONTROLLING_ACCOUNTS, ecdsaKeyPair.getSecond().raw()))
                .thenReturn(ecdsaKeyPair.getFirst().raw());
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ecdsaStatic = mockStatic(EcdsaUtils.class)) {
            int result = cryptoHostFunctions.ecdsaSignPrehashedV1(keyPosition, publicKeyPosition, messagePointer);

            ecdsaStatic.verify(() ->
                    EcdsaUtils.signMessage(ecdsaKeyPair.getFirst().raw(), message));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void ecdsaVerifyV1() {
        when(hostApi.getDataFromMemory(publicKeyPosition, 33)).thenReturn(ecdsaKeyPair.getSecond().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        byte[] sig = EcdsaUtils.signMessage(ecdsaKeyPair.getFirst().raw(), hashedMessage);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(sig);

        int verify = cryptoHostFunctions.ecdsaVerifyV1(signaturePosition, messagePointer, publicKeyPosition);
        assertEquals(1, verify);
    }

    @Test
    void ecdsaVerifyPrehashedV1() {
        when(hostApi.getDataFromMemory(publicKeyPosition, 33)).thenReturn(ecdsaKeyPair.getSecond().raw());
        when(hostApi.getDataFromMemory(keyPosition, 32)).thenReturn(hashedMessage);
        byte[] sig = EcdsaUtils.signMessage(ecdsaKeyPair.getFirst().raw(), hashedMessage);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(sig);

        int verify = cryptoHostFunctions.ecdsaVerifyPrehashedV1(signaturePosition, keyPosition, publicKeyPosition);
        assertEquals(1, verify);
    }

    @Test
    void ecdsaBatchVerifyV1_with_verification_off() {
        when(hostApi.getDataFromMemory(publicKeyPosition, 33)).thenReturn(ecdsaKeyPair.getSecond().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        byte[] sig = EcdsaUtils.signMessage(ecdsaKeyPair.getFirst().raw(), hashedMessage);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(sig);

        assertFalse(cryptoHostFunctions.batchVerificationStarted);

        int verify = cryptoHostFunctions.ecdsaBatchVerifyV1(signaturePosition, messagePointer, publicKeyPosition);

        verifyNoInteractions(signaturesToVerify);
        assertEquals(1, verify);
    }

    @Test
    void ecdsaBatchVerifyV1_with_verification_on() {
        cryptoHostFunctions.batchVerificationStarted = true;
        when(hostApi.getDataFromMemory(publicKeyPosition, 33)).thenReturn(ecdsaKeyPair.getSecond().raw());
        when(hostApi.getDataFromMemory(messagePointer)).thenReturn(message);
        byte[] sig = EcdsaUtils.signMessage(ecdsaKeyPair.getFirst().raw(), hashedMessage);
        when(hostApi.getDataFromMemory(signaturePosition, 64)).thenReturn(sig);

        assertTrue(cryptoHostFunctions.batchVerificationStarted);

        int verify = cryptoHostFunctions.ecdsaBatchVerifyV1(signaturePosition, messagePointer, publicKeyPosition);

        verify(signaturesToVerify).add(any());
        assertEquals(1, verify);
    }

    @Test
    void secp256k1EcdsaRecoverV1() {
        byte[] sig = EcdsaUtils.signMessage(ecdsaKeyPair.getFirst().raw(), hashedMessage);
        when(hostApi.getDataFromMemory(signaturePosition, 65)).thenReturn(sig);
        when(hostApi.getDataFromMemory(keyPosition, 32)).thenReturn(hashedMessage);
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ecdsaStatic = mockStatic(EcdsaUtils.class)) {
            ecdsaStatic.when(() ->
                            EcdsaUtils.recoverPublicKeyFromSignature(sig, hashedMessage, false))
                    .thenReturn(ecdsaKeyPair.getSecond().raw());

            int result = cryptoHostFunctions.secp256k1EcdsaRecoverV1(signaturePosition, keyPosition);

            ecdsaStatic.verify(() ->
                    EcdsaUtils.recoverPublicKeyFromSignature(sig, hashedMessage, false));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void secp256k1EcdsaRecoverCompressedV1() {
        byte[] sig = EcdsaUtils.signMessage(ecdsaKeyPair.getFirst().raw(), hashedMessage);
        when(hostApi.getDataFromMemory(signaturePosition, 65)).thenReturn(sig);
        when(hostApi.getDataFromMemory(keyPosition, 32)).thenReturn(hashedMessage);
        when(hostApi.putDataToMemory(any())).thenReturn(returnPosition);

        try (var ecdsaStatic = mockStatic(EcdsaUtils.class)) {
            ecdsaStatic.when(() ->
                            EcdsaUtils.recoverPublicKeyFromSignature(sig, hashedMessage, true))
                    .thenReturn(ecdsaKeyPair.getSecond().raw());

            int result = cryptoHostFunctions.secp256k1EcdsaRecoverCompressedV1(signaturePosition, keyPosition);

            ecdsaStatic.verify(() ->
                    EcdsaUtils.recoverPublicKeyFromSignature(sig, hashedMessage, true));
            assertEquals(returnPosition, result);
        }
    }

    @Test
    void startBatchVerify() {
        assertFalse(cryptoHostFunctions.batchVerificationStarted);

        cryptoHostFunctions.startBatchVerify();

        assertTrue(cryptoHostFunctions.batchVerificationStarted);
    }

    @Test()
    void finishBatchVerify() {
        cryptoHostFunctions.batchVerificationStarted = true;

        int allValid = cryptoHostFunctions.finishBatchVerify();

        assertEquals(1, allValid);
        assertFalse(cryptoHostFunctions.batchVerificationStarted);
    }
}