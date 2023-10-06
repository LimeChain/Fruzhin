package com.limechain.runtime.hostapi;

import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.hostapi.dto.Signature;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import com.limechain.runtime.hostapi.scale.ResultWriter;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import com.limechain.utils.EcdsaUtils;
import com.limechain.utils.Ed25519Utils;
import com.limechain.utils.HashUtils;
import com.limechain.utils.Sr25519Utils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.reader.StringReader;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.emeraldpay.polkaj.schnorrkel.SchnorrkelException;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.EcdsaKt;
import io.libp2p.crypto.keys.EcdsaPrivateKey;
import io.libp2p.crypto.keys.EcdsaPublicKey;
import io.libp2p.crypto.keys.Ed25519Kt;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import kotlin.Pair;
import org.apache.tomcat.util.buf.HexUtils;
import org.wasmer.ImportObject;
import org.wasmer.Type;
import org.web3j.crypto.Sign;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Implementations of the Crypto HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-crypto-api">Crypto API</a>}
 */
public class CryptoHostFunctions {

    private final KeyStore keyStore;
    private final HostApi hostApi;
    private final List<VerifySignature> signaturesToVerify;
    private boolean batchVerificationStarted = false;

    public CryptoHostFunctions() {
        this.keyStore = AppBean.getBean(KeyStore.class);
        this.hostApi = HostApi.getInstance();
        this.signaturesToVerify = new ArrayList<>();
    }

    public static List<ImportObject> getFunctions() {
        return new CryptoHostFunctions().buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_crypto_ed25519_public_keys_version_1", argv ->
                        ed25519PublicKeysV1((int) argv.get(0)), List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_ed25519_generate_version_1", argv ->
                                ed25519GenerateV1((int) argv.get(0), (long) argv.get(1)),
                        List.of(Type.I32, Type.I64), Type.I32),
                HostApi.getImportObject("ext_crypto_ed25519_sign_version_1", argv ->
                                ed25519SignV1((int) argv.get(0), (int) argv.get(1), (long) argv.get(2)),
                        List.of(Type.I32, Type.I32, Type.I64), Type.I64),
                HostApi.getImportObject("ext_crypto_ed25519_verify_version_1", argv ->
                                ed25519VerifyV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ed25519_batch_verify_version_1", argv ->
                                ed25519BatchVerifyV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_sr25519_public_keys_version_1", argv ->
                        sr25519PublicKeysV1((int) argv.get(0)), List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_sr25519_generate_version_1", argv ->
                                generateSr25519KeyPair((int) argv.get(0), (long) argv.get(1)),
                        List.of(Type.I32, Type.I64), Type.I32),
                HostApi.getImportObject("ext_crypto_sr25519_sign_version_1", argv ->
                                sr25519SignV1((int) argv.get(0), (int) argv.get(1), (long) argv.get(2)),
                        List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_sr25519_verify_version_1", argv ->
                                sr25519VerifyV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_sr25519_verify_version_2", argv ->
                                sr25519VerifyV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_sr25519_batch_verify_version_1", argv ->
                                sr25519BatchVerifyV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ecdsa_public_keys_version_1", argv ->
                        ecdsaPublicKeysV1((int) argv.get(0)), List.of(Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_generate_version_1", argv ->
                                generateEcdsaKeyPair((int) argv.get(0), (long) argv.get(1)),
                        List.of(Type.I32, Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_sign_version_1", argv ->
                                ecdsaSignV1((int) argv.get(0), (int) argv.get(1), (long) argv.get(2)),
                        List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_sign_prehashed_version_1",
                        argv -> ecdsaSignPrehashedV1((int) argv.get(0), (int) argv.get(1), (long) argv.get(2)),
                        List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_verify_version_1", argv ->
                                ecdsaVerifyV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ecdsa_verify_version_2", argv ->
                                ecdsaVerifyV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ecdsa_verify_prehashed_version_1", argv ->
                                ecdsaVerifyPrehashedV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I32, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ecdsa_batch_verify_version_1", argv ->
                                ecdsaBatchVerifyV1((int) argv.get(0), (long) argv.get(1), (int) argv.get(2)),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_secp256k1_ecdsa_recover_version_1", argv ->
                                secp256k1EcdsaRecoverV1((int) argv.get(0), (int) argv.get(1)),
                        List.of(Type.I32, Type.I32), (Type.I64)),
                HostApi.getImportObject("ext_crypto_secp256k1_ecdsa_recover_version_2", argv ->
                                secp256k1EcdsaRecoverV1((int) argv.get(0), (int) argv.get(1)),
                        List.of(Type.I32, Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_secp256k1_ecdsa_recover_compressed_version_1",
                        argv -> secp256k1EcdsaRecoverCompressedV1((int) argv.get(0), (int) argv.get(1)),
                        List.of(Type.I32, Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_secp256k1_ecdsa_recover_compressed_version_2",
                        argv -> secp256k1EcdsaRecoverCompressedV1((int) argv.get(0), (int) argv.get(1)),
                        List.of(Type.I32, Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_start_batch_verify", argv ->
                        startBatchVerify(), HostApi.EMPTY_LIST_OF_TYPES),
                HostApi.getImportObject("ext_crypto_finish_batch_verify", argv ->
                        finishBatchVerify(), HostApi.EMPTY_LIST_OF_TYPES, Type.I32));
    }

    private VerifySignature internalGetVerifySignature(int signature, long message, int publicKey, Key key) {
        final byte[] signatureData = hostApi.getDataFromMemory(signature, 64);
        final byte[] messageData = hostApi.getDataFromMemory(new RuntimePointerSize(message));
        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 32);
        return new VerifySignature(signatureData, messageData, publicKeyData, key);
    }

    private int ed25519PublicKeysV1(int keyTypeId) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));

        if (keyType == null || keyType.getKey() != Key.ED25519 && keyType.getKey() != Key.GENERIC) {
            //Todo: How to handle exceptions?
            return -1;
        }

        byte[] publicKeys = keyStore.getPublicKeysByKeyType(keyType);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos);
        try {
            scaleWriter.writeAsList(publicKeys);
        } catch (IOException e) {
            //Todo: How to handle exceptions?
            return -1;
        }

        return hostApi.putDataToMemory(baos.toByteArray());
    }

    private int ed25519GenerateV1(int keyTypeId, long seed) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));
        final byte[] seedData = hostApi.getDataFromMemory(new RuntimePointerSize(seed));
        Optional<String> seedString = new ScaleCodecReader(seedData).readOptional(new StringReader());

        final Ed25519PrivateKey ed25519PrivateKey;
        if (seedString.isPresent()) {
            ed25519PrivateKey = Ed25519Utils.generateKeyPair(seedData);
        } else {
            ed25519PrivateKey = Ed25519Utils.generateKeyPair();
        }
        final PubKey pubKey = ed25519PrivateKey.publicKey();

        keyStore.put(keyType, pubKey.raw(), ed25519PrivateKey.raw());
        return hostApi.putDataToMemory(pubKey.raw());
    }

    private long ed25519SignV1(int keyTypeId, int publicKey, long message) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos);

        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 32);
        final byte[] messageData = hostApi.getDataFromMemory(new RuntimePointerSize(message));

        byte[] privateKey = keyStore.get(keyType, publicKeyData);
        Signature signature = new Signature(publicKeyData, messageData, privateKey);
        try {
            if (privateKey == null) {
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.empty());
            } else {
                byte[] signed = Ed25519Kt.unmarshalEd25519PrivateKey(signature.getPrivateKey()).sign(signature.getMessageData());
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, signed);
            }
        } catch (IOException e) {
            //Todo: How to handle exceptions?
            return -1;
        }

        return hostApi.putDataToMemory(baos.toByteArray());
    }

    private int ed25519VerifyV1(int signature, long message, int publicKey) {
        VerifySignature result = internalGetVerifySignature(signature, message, publicKey, Key.ED25519);
        return internalVerifyEd25519Sig(result);
    }


    private int ed25519BatchVerifyV1(int signature, long message, int publicKey) {
        VerifySignature result = internalGetVerifySignature(signature, message, publicKey, Key.ED25519);

        if(batchVerificationStarted){
            signaturesToVerify.add(result);
        }else{
            return internalVerifyEd25519Sig(result);
        }
        return 1;
    }

    private static int internalVerifyEd25519Sig(VerifySignature result) {
        final PubKey pubKey = Ed25519Kt.unmarshalEd25519PublicKey(result.getPublicKeyData());
        final boolean verified = pubKey.verify(result.getMessageData(), result.getSignatureData());

        return verified ? 0 : 1;
    }

    private Number sr25519PublicKeysV1(int keyTypeId) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));

        if (keyType == null || keyType.getKey() != Key.SR25519 && keyType.getKey() != Key.GENERIC) {
            //Todo: How to handle exceptions?
            return -1;
        }

        byte[] publicKeys = keyStore.getPublicKeysByKeyType(keyType);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos);
        try {
            scaleWriter.writeAsList(publicKeys);
        } catch (IOException e) {
            //Todo: How to handle exceptions?
            return -1;
        }

        return hostApi.putDataToMemory(baos.toByteArray());
    }

    private int generateSr25519KeyPair(int keyTypeId, long seed) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));
        final byte[] seedData = hostApi.getDataFromMemory(new RuntimePointerSize(seed));
        Optional<String> seedString = new ScaleCodecReader(seedData).readOptional(ScaleCodecReader::readString);
        final Schnorrkel.KeyPair keyPair;

        if (seedString.isPresent()) {
            keyPair = Sr25519Utils.generateKeyPair(HexUtils.fromHexString(seedString.get()));
        } else {
            keyPair = Sr25519Utils.generateKeyPair();
        }

        keyStore.put(keyType, keyPair.getPublicKey(), keyPair.getSecretKey());
        return hostApi.putDataToMemory(keyPair.getPublicKey());
    }

    private Number sr25519SignV1(int keyTypeId, int publicKey, long message) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos);

        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 32);
        final byte[] messageData = hostApi.getDataFromMemory(new RuntimePointerSize(message));

        byte[] privateKey = keyStore.get(keyType, publicKeyData);
        Signature signature = new Signature(publicKeyData, messageData, privateKey);
        try {
            if (privateKey == null) {
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.empty());
            } else {
                Schnorrkel.KeyPair keyPair = new Schnorrkel.KeyPair(signature.getPublicKeyData(), signature.getPrivateKey());
                byte[] signed = Schnorrkel.getInstance().sign(signature.getMessageData(), keyPair);
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, signed);
            }
        } catch (IOException | SchnorrkelException e) {
            //Todo: How to handle exceptions?
            return -1;
        }

        return hostApi.putDataToMemory(baos.toByteArray());
    }

    private Number sr25519VerifyV1(int signature, long message, int publicKey) {
        VerifySignature verifiedSignature = internalGetVerifySignature(signature, message, publicKey, Key.SR25519);

        return internalVerifySr25519Sig(verifiedSignature);
    }

    private int sr25519BatchVerifyV1(int signature, long message, int publicKey) {
        VerifySignature verifiedSignature = internalGetVerifySignature(signature, message, publicKey, Key.SR25519);

        if(batchVerificationStarted){
            signaturesToVerify.add(verifiedSignature);
        }else{
            return internalVerifySr25519Sig(verifiedSignature);
        }

        return 1;
    }

    private static int internalVerifySr25519Sig(final VerifySignature signature) {
        boolean verified = false;
        try {
            Schnorrkel schnorrkel = Schnorrkel.getInstance();
            Schnorrkel.PublicKey publicKey = new Schnorrkel.PublicKey(signature.getPublicKeyData());
            verified = schnorrkel.verify(signature.getSignatureData(), signature.getMessageData(), publicKey);
        } catch (SchnorrkelException e) {
            //Todo: How to handle exceptions
            return 0;
        }

        return verified ? 1 : 0;
    }


    private Number ecdsaPublicKeysV1(int keyTypeId) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));

        if (keyType == null || keyType.getKey() != Key.GENERIC) {
            //Todo: How to handle exceptions?
            return -1;
        }

        byte[] publicKeys = keyStore.getPublicKeysByKeyType(keyType);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos);
        try {
            scaleWriter.writeAsList(publicKeys);
        } catch (IOException e) {
            //Todo: How to handle exceptions?
            return -1;
        }

        return hostApi.putDataToMemory(baos.toByteArray());
    }

    public int generateEcdsaKeyPair(int keyTypeId, long seed) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));
        final byte[] seedData = hostApi.getDataFromMemory(new RuntimePointerSize(seed));
        Optional<String> seedString = new ScaleCodecReader(seedData).readOptional(new StringReader());

        final Pair<EcdsaPrivateKey, EcdsaPublicKey> keyPair = seedString
                .map(EcdsaUtils::generateKeyPair)
                .orElseGet(EcdsaUtils::generateKeyPair);

        keyStore.put(keyType, keyPair.getSecond().raw(), keyPair.getFirst().raw());
        return hostApi.putDataToMemory(keyPair.getSecond().raw());
    }

    private Number ecdsaSignV1(int keyTypeId, int publicKey, long message) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos);

        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 33);
        byte[] messageData = hostApi.getDataFromMemory(new RuntimePointerSize(message));
        byte[] hashedMessage = HashUtils.hashWithBlake2b(messageData);

        byte[] privateKey = keyStore.get(keyType, publicKeyData);
        try {
            if (privateKey == null) {
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.empty());
            } else {
                byte[] signed = EcdsaKt.unmarshalEcdsaPrivateKey(privateKey).sign(hashedMessage);
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, signed);
            }
        } catch (IOException e) {
            //Todo: How to handle exceptions?
            return -1;
        }

        return hostApi.putDataToMemory(baos.toByteArray());
    }

    private Number ecdsaSignPrehashedV1(int keyTypeId, int publicKey, long message) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, 4));

        if (keyType == null || keyType.getKey() != Key.ECDSA && keyType.getKey() != Key.GENERIC) {
            //Todo: How to handle exceptions?
            return -1;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos);

        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 33);
        byte[] messageData = hostApi.getDataFromMemory(new RuntimePointerSize(message));

        byte[] privateKey = keyStore.get(keyType, publicKeyData);
        try {
            if (privateKey == null) {
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.empty());
            } else {
                byte[] signed = EcdsaKt.unmarshalEcdsaPrivateKey(privateKey).sign(messageData);
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, signed);
            }
        } catch (IOException e) {
            //Todo: How to handle exceptions?
            return -1;
        }

        return hostApi.putDataToMemory(baos.toByteArray());
    }

    private Number ecdsaVerifyV1(int signature, long message, int publicKey) {
        final VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ECDSA);
        verifySig.setMessageData(HashUtils.hashWithBlake2b(verifySig.getMessageData()));
        return internalVerifyEcdsaSig(verifySig);
    }

    private int ecdsaVerifyPrehashedV1(int signature, long message, int publicKey) {
        final VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ECDSA);
        return internalVerifyEcdsaSig(verifySig);
    }

    private int ecdsaBatchVerifyV1(int signature, long message, int publicKey) {
        final VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ECDSA);
        verifySig.setMessageData(HashUtils.hashWithBlake2b(verifySig.getMessageData()));

        if(batchVerificationStarted){
            signaturesToVerify.add(verifySig);
        }else{
            return internalVerifyEcdsaSig(verifySig);
        }
        return 1;
    }

    private static int internalVerifyEcdsaSig(final VerifySignature signature) {
        final EcdsaPublicKey pubKey = EcdsaKt.unmarshalEcdsaPublicKey(signature.getPublicKeyData());
        final boolean verified = pubKey.verify(signature.getMessageData(), signature.getSignatureData());

        return verified ? 0 : 1;
    }

    private int secp256k1EcdsaRecoverV1(int signature, int message) {
        EcdsaPublicKey ecdsaPublicKey = internalSecp256k1RecoverKey(signature, message);
        byte[] uncompressedBytes = ecdsaPublicKey.toUncompressedBytes();
        return secp2561kScaleKeyResult(uncompressedBytes);
    }

    private int secp256k1EcdsaRecoverCompressedV1(int signature, int message) {
        EcdsaPublicKey ecdsaPublicKey = internalSecp256k1RecoverKey(signature, message);
        byte[] rawBytes = ecdsaPublicKey.raw();
        return secp2561kScaleKeyResult(rawBytes);
    }

    private int secp2561kScaleKeyResult(byte[] rawBytes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ScaleCodecWriter scaleCodecWriter = new ScaleCodecWriter(baos);
        ResultWriter resultWriter = new ResultWriter();
        try {
            resultWriter.writeResult(scaleCodecWriter, true);
            resultWriter.write(scaleCodecWriter, rawBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return hostApi.putDataToMemory(baos.toByteArray());
    }

    private EcdsaPublicKey internalSecp256k1RecoverKey(int signature, int message) {
        final byte[] messageData = hostApi.getDataFromMemory(message, 32);
        final byte[] signatureData = hostApi.getDataFromMemory(signature, 65);

        if (signatureData[64] >= 27) {
            signatureData[64] -= 27;
        }
        byte v = signatureData[64];
        byte[] r = Arrays.copyOfRange(signatureData, 0, 32);
        byte[] s = Arrays.copyOfRange(signatureData, 32, 64);

        BigInteger key = null;
        try {
            key = Sign.signedMessageToKey(messageData, new Sign.SignatureData(v, r, s));
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }

         return EcdsaKt.unmarshalEcdsaPublicKey(key.toByteArray());
    }

    private void startBatchVerify() {
        batchVerificationStarted = true;
    }

    private int finishBatchVerify() {
        if(!batchVerificationStarted){
            throw new RuntimeException("Batch verification not started");
            //TODO: panic?
        }
        batchVerificationStarted = false;
        int allValid = 1;
        for (Iterator<VerifySignature> iterator = signaturesToVerify.iterator(); iterator.hasNext(); ) {
            VerifySignature verifySignature = iterator.next();
            switch (verifySignature.getKey()) {
                case ECDSA -> allValid &= internalVerifyEcdsaSig(verifySignature);
                case ED25519 -> allValid &= internalVerifyEd25519Sig(verifySignature);
                case SR25519 -> allValid &= internalVerifySr25519Sig(verifySignature);
                case GENERIC -> allValid &= 1;
            }
            iterator.remove();
        }
        return allValid;
    }

}
