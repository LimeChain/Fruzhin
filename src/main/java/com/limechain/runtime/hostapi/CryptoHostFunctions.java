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
import io.emeraldpay.polkaj.scale.writer.ListWriter;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.wasmer.ImportObject;
import org.wasmer.Type;
import org.wasmer.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementations of the Crypto HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-crypto-api">Crypto API</a>}
 */
public class CryptoHostFunctions {

    public static final String SCALE_ENCODING_SIGNED_MESSAGE_ERROR = "Error while SCALE encoding signed message";
    public static final String INVALID_KEY_TYPE = "Invalid key type";
    public static final int KEY_TYPE_LEN = 4;
    private final KeyStore keyStore;
    private final HostApi hostApi;
    private final Set<VerifySignature> signaturesToVerify;
    private boolean batchVerificationStarted = false;

    public CryptoHostFunctions() {
        this.keyStore = AppBean.getBean(KeyStore.class);
        this.hostApi = HostApi.getInstance();
        this.signaturesToVerify = new HashSet<>();
    }

    public static List<ImportObject> getFunctions() {
        return new CryptoHostFunctions().buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_crypto_ed25519_public_keys_version_1", argv ->
                        ed25519PublicKeysV1(argv.get(0).intValue()), List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_ed25519_generate_version_1", argv ->
                                ed25519GenerateV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1))),
                        List.of(Type.I32, Type.I64), Type.I32),
                HostApi.getImportObject("ext_crypto_ed25519_sign_version_1", argv ->
                                ed25519SignV1(argv.get(0).intValue(), argv.get(1).intValue(),
                                        new RuntimePointerSize(argv.get(2))),
                        List.of(Type.I32, Type.I32, Type.I64), Type.I64),
                HostApi.getImportObject("ext_crypto_ed25519_verify_version_1", argv ->
                                ed25519VerifyV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ed25519_batch_verify_version_1", argv ->
                                ed25519BatchVerifyV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_sr25519_public_keys_version_1", argv ->
                        sr25519PublicKeysV1(argv.get(0).intValue()), List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_sr25519_generate_version_1", argv ->
                                generateSr25519KeyPair(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1))),
                        List.of(Type.I32, Type.I64), Type.I32),
                HostApi.getImportObject("ext_crypto_sr25519_sign_version_1", argv ->
                                sr25519SignV1(argv.get(0).intValue(), argv.get(1).intValue(),
                                        new RuntimePointerSize(argv.get(2))),
                        List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_sr25519_verify_version_1", argv ->
                                sr25519VerifyV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_sr25519_verify_version_2", argv ->
                                sr25519VerifyV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_sr25519_batch_verify_version_1", argv ->
                                sr25519BatchVerifyV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ecdsa_public_keys_version_1", argv ->
                        ecdsaPublicKeysV1(argv.get(0).intValue()), List.of(Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_generate_version_1", argv ->
                                generateEcdsaKeyPair(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1))),
                        List.of(Type.I32, Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_sign_version_1", argv ->
                                ecdsaSignV1(argv.get(0).intValue(), argv.get(1).intValue(),
                                        new RuntimePointerSize(argv.get(2))),
                        List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_sign_prehashed_version_1",
                        argv -> ecdsaSignPrehashedV1(argv.get(0).intValue(), argv.get(1).intValue(),
                                new RuntimePointerSize(argv.get(2))),
                        List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_verify_version_1", argv ->
                                ecdsaVerifyV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ecdsa_verify_version_2", argv ->
                                ecdsaVerifyV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ecdsa_verify_prehashed_version_1", argv ->
                                ecdsaVerifyPrehashedV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I32, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_ecdsa_batch_verify_version_1", argv ->
                                ecdsaBatchVerifyV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()),
                        List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_crypto_secp256k1_ecdsa_recover_version_1", argv ->
                                secp256k1EcdsaRecoverV1(argv.get(0).intValue(), argv.get(1).intValue()),
                        List.of(Type.I32, Type.I32), (Type.I64)),
                HostApi.getImportObject("ext_crypto_secp256k1_ecdsa_recover_version_2", argv ->
                                secp256k1EcdsaRecoverV1(argv.get(0).intValue(), argv.get(1).intValue()),
                        List.of(Type.I32, Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_secp256k1_ecdsa_recover_compressed_version_1",
                        argv -> secp256k1EcdsaRecoverCompressedV1(argv.get(0).intValue(), argv.get(1).intValue()),
                        List.of(Type.I32, Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_secp256k1_ecdsa_recover_compressed_version_2",
                        argv -> secp256k1EcdsaRecoverCompressedV1(argv.get(0).intValue(), argv.get(1).intValue()),
                        List.of(Type.I32, Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_start_batch_verify", argv ->
                        startBatchVerify(), HostApi.EMPTY_LIST_OF_TYPES),
                HostApi.getImportObject("ext_crypto_finish_batch_verify", argv ->
                        finishBatchVerify(), HostApi.EMPTY_LIST_OF_TYPES, Type.I32));
    }

    private VerifySignature internalGetVerifySignature(int signature, RuntimePointerSize message,
                                                       int publicKey, Key key) {
        final byte[] signatureData = hostApi.getDataFromMemory(signature, 64);
        final byte[] messageData = hostApi.getDataFromMemory(message);
        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 32);
        return new VerifySignature(signatureData, messageData, publicKeyData, key);
    }

    private int ed25519PublicKeysV1(int keyTypeId) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));

        if (keyType == null || (keyType.getKey() != Key.ED25519 && keyType.getKey() != Key.GENERIC)) {
            throw new RuntimeException(INVALID_KEY_TYPE);
        }

        return internalPublicKeys(keyType);
    }

    private int ed25519GenerateV1(int keyTypeId, RuntimePointerSize seed) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));
        final byte[] seedData = hostApi.getDataFromMemory(seed);
        Optional<String> seedString = new ScaleCodecReader(seedData).readOptional(new StringReader());

        final Ed25519PrivateKey ed25519PrivateKey = seedString
                .map(Ed25519Utils::generateKeyPair)
                .orElseGet(Ed25519Utils::generateKeyPair);

        final PubKey pubKey = ed25519PrivateKey.publicKey();

        keyStore.put(keyType, pubKey.raw(), ed25519PrivateKey.raw());
        return hostApi.putDataToMemory(pubKey.raw());
    }

    private long ed25519SignV1(int keyTypeId, int publicKey, RuntimePointerSize message) {
        final Signature sig = internalGetSignData(keyTypeId, publicKey, message);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos)) {

            if (sig.getPrivateKey() == null) {
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.empty());
                return hostApi.putDataToMemory(baos.toByteArray());
            }

            byte[] signed = Ed25519Utils.signMessage(sig.getPrivateKey(), sig.getMessageData());
            scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.ofNullable(signed));
            return hostApi.putDataToMemory(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(SCALE_ENCODING_SIGNED_MESSAGE_ERROR);
        }

    }

    @NotNull
    private Signature internalGetSignData(int keyTypeId, int publicKey, RuntimePointerSize message) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));

        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 32);
        final byte[] messageData = hostApi.getDataFromMemory(message);

        byte[] privateKey = keyStore.get(keyType, publicKeyData);
        return new Signature(publicKeyData, messageData, privateKey);
    }

    private int ed25519VerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ED25519);
        return Ed25519Utils.verifySignature(verifySig) ? 1 : 0;
    }

    private int ed25519BatchVerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ED25519);

        if (batchVerificationStarted) {
            signaturesToVerify.add(verifySig);
            return 1;
        } else {
            return Ed25519Utils.verifySignature(verifySig) ? 1 : 0;
        }
    }

    private int sr25519PublicKeysV1(int keyTypeId) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));

        if (keyType == null || (keyType.getKey() != Key.SR25519 && keyType.getKey() != Key.GENERIC)) {
            throw new RuntimeException(INVALID_KEY_TYPE);
        }

        return internalPublicKeys(keyType);
    }

    private int generateSr25519KeyPair(int keyTypeId, RuntimePointerSize seed) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));
        final byte[] seedData = hostApi.getDataFromMemory(seed);
        Optional<String> seedString = new ScaleCodecReader(seedData).readOptional(ScaleCodecReader::readString);
        final Schnorrkel.KeyPair keyPair;

        if (seedString.isPresent()) {
            keyPair = Sr25519Utils.generateKeyPair(seedString.get());
        } else {
            keyPair = Sr25519Utils.generateKeyPair();
        }

        keyStore.put(keyType, keyPair.getPublicKey(), keyPair.getSecretKey());
        return hostApi.putDataToMemory(keyPair.getPublicKey());
    }

    private Number sr25519SignV1(int keyTypeId, int publicKey, RuntimePointerSize message) {
        Signature sig = internalGetSignData(keyTypeId, publicKey, message);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos)) {

            if (sig.getPrivateKey() == null) {
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.empty());
                return hostApi.putDataToMemory(baos.toByteArray());
            }

            byte[] signed = Sr25519Utils.signMessage(sig.getPublicKeyData(), sig.getPrivateKey(), sig.getMessageData());
            scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.of(signed));
            return hostApi.putDataToMemory(baos.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(SCALE_ENCODING_SIGNED_MESSAGE_ERROR);
        }

    }

    private Number sr25519VerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        VerifySignature verifiedSignature = internalGetVerifySignature(signature, message, publicKey, Key.SR25519);

        return Sr25519Utils.verifySignature(verifiedSignature) ? 1 : 0;
    }

    private int sr25519BatchVerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        VerifySignature verifiedSignature = internalGetVerifySignature(signature, message, publicKey, Key.SR25519);

        if (batchVerificationStarted) {
            signaturesToVerify.add(verifiedSignature);
        } else {
            return Sr25519Utils.verifySignature(verifiedSignature) ? 1 : 0;
        }

        return 1;
    }

    private Number ecdsaPublicKeysV1(int keyTypeId) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));

        if (keyType == null || keyType.getKey() != Key.GENERIC) {
            throw new RuntimeException(INVALID_KEY_TYPE);
        }

        return internalPublicKeys(keyType);
    }

    private int internalPublicKeys(KeyType keyType) {
        List<byte[]> publicKeys = keyStore.getPublicKeysByKeyType(keyType);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos)) {

            ListWriter<byte[]> listWriter = new ListWriter<>(ScaleCodecWriter::writeByteArray);
            listWriter.write(scaleWriter, publicKeys);
            return hostApi.putDataToMemory(baos.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error while SCALE encoding public keys");
        }
    }

    public int generateEcdsaKeyPair(int keyTypeId, RuntimePointerSize seed) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));
        final byte[] seedData = hostApi.getDataFromMemory(seed);
        Optional<String> seedString = new ScaleCodecReader(seedData).readOptional(new StringReader());

        final Pair<PrivKey, PubKey> keyPair = seedString
                .map(EcdsaUtils::generateKeyPair)
                .orElseGet(EcdsaUtils::generateKeyPair);

        keyStore.put(keyType, keyPair.getSecond().raw(), keyPair.getFirst().raw());
        return hostApi.putDataToMemory(keyPair.getSecond().raw());
    }

    private Number ecdsaSignV1(int keyTypeId, int publicKey, RuntimePointerSize message) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));

        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 33);
        byte[] messageData = hostApi.getDataFromMemory(message);
        byte[] hashedMessage = HashUtils.hashWithBlake2b(messageData);

        byte[] privateKey = keyStore.get(keyType, publicKeyData);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos)) {

            if (privateKey == null) {
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.empty());
                return hostApi.putDataToMemory(baos.toByteArray());
            }

            byte[] signed = EcdsaUtils.signMessage(privateKey, hashedMessage);
            scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.of(signed));
            return hostApi.putDataToMemory(baos.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(SCALE_ENCODING_SIGNED_MESSAGE_ERROR);
        }
    }

    private Number ecdsaSignPrehashedV1(int keyTypeId, int publicKey, RuntimePointerSize message) {
        final KeyType keyType = KeyType.getByBytes(hostApi.getDataFromMemory(keyTypeId, KEY_TYPE_LEN));

        if (keyType == null || (keyType.getKey() != Key.ECDSA && keyType.getKey() != Key.GENERIC)) {
            throw new RuntimeException(INVALID_KEY_TYPE);
        }

        final byte[] publicKeyData = hostApi.getDataFromMemory(publicKey, 33);
        byte[] messageData = hostApi.getDataFromMemory(message);

        byte[] privateKey = keyStore.get(keyType, publicKeyData);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos)) {

            if (privateKey == null) {
                scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.empty());
                return hostApi.putDataToMemory(baos.toByteArray());
            }

            byte[] signed = EcdsaUtils.signMessage(privateKey, messageData);
            scaleWriter.writeOptional(ScaleCodecWriter::writeByteArray, Optional.of(signed));
            return hostApi.putDataToMemory(baos.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(SCALE_ENCODING_SIGNED_MESSAGE_ERROR);
        }
    }

    private Number ecdsaVerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        final VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ECDSA);
        verifySig.setMessageData(HashUtils.hashWithBlake2b(verifySig.getMessageData()));
        return EcdsaUtils.verifySignature(verifySig) ? 1 : 0;
    }

    private int ecdsaVerifyPrehashedV1(int signature, RuntimePointerSize message, int publicKey) {
        final VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ECDSA);
        return EcdsaUtils.verifySignature(verifySig) ? 1 : 0;
    }

    private int ecdsaBatchVerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        final VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ECDSA);
        verifySig.setMessageData(HashUtils.hashWithBlake2b(verifySig.getMessageData()));

        if (batchVerificationStarted) {
            signaturesToVerify.add(verifySig);
        } else {
            return EcdsaUtils.verifySignature(verifySig) ? 1 : 0;
        }
        return 1;
    }

    private int secp256k1EcdsaRecoverV1(int signature, int message) {
        byte[] ecdsaPublicKey = internalSecp256k1RecoverKey(signature, message, false);
        return secp2561kScaleKeyResult(ecdsaPublicKey);
    }

    private int secp256k1EcdsaRecoverCompressedV1(int signature, int message) {
        byte[] rawBytes = internalSecp256k1RecoverKey(signature, message, true);
        return secp2561kScaleKeyResult(rawBytes);
    }

    private int secp2561kScaleKeyResult(byte[] rawBytes) {
        ResultWriter resultWriter = new ResultWriter();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ScaleCodecWriter scaleCodecWriter = new ScaleCodecWriter(baos)) {

            resultWriter.writeResult(scaleCodecWriter, true);
            resultWriter.write(scaleCodecWriter, rawBytes);
            return hostApi.putDataToMemory(baos.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(SCALE_ENCODING_SIGNED_MESSAGE_ERROR);
        }
    }

    private byte[] internalSecp256k1RecoverKey(int signature, int message, boolean compressed) {
        final byte[] messageData = hostApi.getDataFromMemory(message, 32);
        final byte[] signatureData = hostApi.getDataFromMemory(signature, 65);

        return EcdsaUtils.recoverPublicKeyFromSignature(signatureData, messageData, compressed);
    }

    private void startBatchVerify() {
        batchVerificationStarted = true;
    }

    private int finishBatchVerify() {
        if (!batchVerificationStarted) {
            Util.nativePanic("Batch verification not started");
            throw new RuntimeException("Batch verification not started");
        }
        batchVerificationStarted = false;
        HashSet<VerifySignature> signatures = new HashSet<>(signaturesToVerify);
        signaturesToVerify.clear();

        boolean allValid = signatures.stream().allMatch(signature -> {
            switch (signature.getKey()) {
                case ECDSA -> {
                    return EcdsaUtils.verifySignature(signature);
                }
                case ED25519 -> {
                    return Ed25519Utils.verifySignature(signature);
                }
                case SR25519 -> {
                    return Sr25519Utils.verifySignature(signature);
                }
                case GENERIC -> {
                    return false;
                }
            }
            return false;
        });

        return allValid ? 1 : 0;
    }

}
