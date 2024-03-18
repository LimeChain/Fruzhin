package com.limechain.runtime.hostapi;

import com.limechain.exception.BatchVerificationNotStartedException;
import com.limechain.exception.InvalidKeyTypeException;
import com.limechain.exception.InvalidSeedException;
import com.limechain.runtime.Runtime;
import com.limechain.utils.scale.exceptions.ScaleEncodingException;
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
import io.emeraldpay.polkaj.scale.writer.ListWriter;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.crypto.PubKey;
import io.libp2p.crypto.keys.Ed25519PrivateKey;
import kotlin.Pair;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.wasmer.ImportObject;
import org.wasmer.Type;
import org.wasmer.Util;
import org.web3j.crypto.MnemonicUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementations of the Crypto HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-crypto-api">Crypto API</a>}
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CryptoHostFunctions {
    public static final String SCALE_ENCODING_SIGNED_MESSAGE_ERROR = "Error while SCALE encoding signed message";
    public static final String INVALID_KEY_TYPE = "Invalid key type";
    public static final String SEED_IS_INVALID = "Seed is invalid";
    public static final String BATCH_VERIFICATION_NOT_STARTED = "Batch verification not started";
    public static final int PUBLIC_KEY_LEN = 32;
    public static final int SIGNATURE_LEN = 64;
    private static final String TYPE_RECEIVED_STRING = "Type received: %s";
    private final KeyStore keyStore;
    private final Runtime runtime;
    private final Set<VerifySignature> signaturesToVerify;
    protected boolean batchVerificationStarted = false;

    private CryptoHostFunctions(Runtime runtime) {
        this(AppBean.getBean(KeyStore.class), runtime, new HashSet<>());
    }

    public static List<ImportObject> getFunctions(Runtime runtime) {
        return new CryptoHostFunctions(runtime).buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_crypto_ed25519_public_keys_version_1", argv ->
                        ed25519PublicKeysV1(argv.get(0).intValue()).pointerSize(), List.of(Type.I32), Type.I64),
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
                        sr25519PublicKeysV1(argv.get(0).intValue()).pointerSize(), List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_crypto_sr25519_generate_version_1", argv ->
                                sr25519GenerateV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1))),
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
                        ecdsaPublicKeysV1(argv.get(0).intValue()).pointerSize(), List.of(Type.I64), (Type.I64)),
                HostApi.getImportObject("ext_crypto_ecdsa_generate_version_1", argv ->
                                ecdsaGenerateV1(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1))),
                        List.of(Type.I32, Type.I64), (Type.I32)),
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
                                ecdsaVerifyPrehashedV1(argv.get(0).intValue(), argv.get(1).intValue(),
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
                HostApi.getImportObject("ext_crypto_start_batch_verify_version_1", argv ->
                        startBatchVerify(), HostApi.EMPTY_LIST_OF_TYPES),
                HostApi.getImportObject("ext_crypto_finish_batch_verify_version_1", argv ->
                        finishBatchVerify(), HostApi.EMPTY_LIST_OF_TYPES, Type.I32));
    }

    private VerifySignature internalGetVerifySignature(int signature, RuntimePointerSize message,
                                                       int publicKey, Key key) {
        final byte[] signatureData = runtime.getDataFromMemory(new RuntimePointerSize(signature, SIGNATURE_LEN));
        final byte[] messageData = runtime.getDataFromMemory(message);
        final byte[] publicKeyData = runtime.getDataFromMemory(new RuntimePointerSize(publicKey, key == Key.ECDSA ?
                EcdsaUtils.PUBLIC_KEY_COMPRESSED_LEN : PUBLIC_KEY_LEN));
        return new VerifySignature(signatureData, messageData, publicKeyData, key);
    }

    /**
     * Returns all ed25519 public keys for the given key identifier from the keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @return a pointer-size to the SCALE encoded array of 256bit public keys.
     */
    public RuntimePointerSize ed25519PublicKeysV1(int keyTypeId) {
        byte[] keyTypeBytes = runtime.getDataFromMemory(new RuntimePointerSize(keyTypeId, KeyType.KEY_TYPE_LEN));
        final KeyType keyType = KeyType.getByBytes(keyTypeBytes);

        if (keyType == null || (keyType.getKey() != Key.ED25519 && keyType.getKey() != Key.GENERIC)) {
            throw new InvalidKeyTypeException(
                    String.format(TYPE_RECEIVED_STRING, keyType != null ? keyType.getKey() : null ));
        }

        return internalPublicKeys(keyType);
    }

    /**
     * Generates an ed25519 key for the given key type using an optional BIP-39 seed and stores it in the keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @param seed      a pointer-size to the SCALE encoded Option value containing the BIP-39 seed which must be valid
     *                  UTF8.
     * @return a pointer to the buffer containing the 256-bit public key.
     * @throws InvalidSeedException Panics if the key cannot be generated, such as when an invalid key type or invalid
     *         seed was provided.
     */
    public int ed25519GenerateV1(int keyTypeId, RuntimePointerSize seed) {
        var pair = getSeedStringAndKeyType(keyTypeId, seed);
        final Ed25519PrivateKey ed25519PrivateKey;
        if (pair.getSeed() != null) {
            if (!MnemonicUtils.validateMnemonic(pair.getSeed())) {
                Util.nativePanic(SEED_IS_INVALID);
                throw new InvalidSeedException();
            }
            ed25519PrivateKey = Ed25519Utils.generateKeyPair(pair.getSeed());
        } else {
            ed25519PrivateKey = Ed25519Utils.generateKeyPair();
        }

        final PubKey pubKey = ed25519PrivateKey.publicKey();

        keyStore.put(pair.getKeyType(), pubKey.raw(), ed25519PrivateKey.raw());
        return runtime.writeDataToMemory(pubKey.raw()).pointer();
    }

    private SeedStringKeyTypePair getSeedStringAndKeyType(int keyTypeId, RuntimePointerSize seed) {
        byte[] keyTypeBytes = runtime.getDataFromMemory(new RuntimePointerSize(keyTypeId, KeyType.KEY_TYPE_LEN));
        final KeyType keyType = KeyType.getByBytes(keyTypeBytes);
        if (keyType == null) {
            Util.nativePanic(INVALID_KEY_TYPE);
            throw new InvalidKeyTypeException("Null is not a valid type");
        }
        final byte[] seedData = runtime.getDataFromMemory(seed);
        return new SeedStringKeyTypePair(new ScaleCodecReader(seedData).readOptional(ScaleCodecReader::readString)
                .orElse(null), keyType);
    }

    /**
     * Signs the given message with the ed25519 key that corresponds to the given public key and key type in the
     * keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @param publicKey a pointer to the buffer containing the 256 bit public key.
     * @param message   a pointer-size to the message that is to be signed.
     * @return a pointer-size to the SCALE encoded Option value containing the 64-byte signature.
     * This function returns if the public key cannot be found in the key store.
     */
    public long ed25519SignV1(int keyTypeId, int publicKey, RuntimePointerSize message) {
        final Signature sig = internalGetSignData(keyTypeId, publicKey, message, Key.ED25519);

        byte[] signed = null;
        if (sig.getPrivateKey() != null) {
            signed = Ed25519Utils.signMessage(sig.getPrivateKey(), sig.getMessageData());
        }

        return runtime.writeDataToMemory(scaleEncodedOption(signed)).pointer();
    }

    @NotNull
    private Signature internalGetSignData(int keyTypeId, int publicKey, RuntimePointerSize message, Key key) {
        byte[] keyTypeBytes = runtime.getDataFromMemory(new RuntimePointerSize(keyTypeId, KeyType.KEY_TYPE_LEN));
        final KeyType keyType = KeyType.getByBytes(keyTypeBytes);

        final byte[] publicKeyData = runtime.getDataFromMemory(new RuntimePointerSize(publicKey, key == Key.ECDSA ?
                EcdsaUtils.PUBLIC_KEY_COMPRESSED_LEN : PUBLIC_KEY_LEN));
        final byte[] messageData = runtime.getDataFromMemory(message);

        byte[] privateKey = keyStore.get(keyType, publicKeyData);
        return new Signature(publicKeyData, messageData, privateKey);
    }

    /**
     * Verifies an ed25519 signature.
     *
     * @param signature a pointer to the buffer containing the 64-byte signature.
     * @param message   a pointer-size to the message that is to be verified.
     * @param publicKey a pointer to the buffer containing the 256-bit public key.
     * @return a i32 integer value equal to 1 if the signature is valid or a value equal to 0 if otherwise.
     */
    public int ed25519VerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ED25519);
        return Ed25519Utils.verifySignature(verifySig) ? 1 : 0;
    }

    /**
     * Registers an ed25519 signature for batch verification. Batch verification is enabled by calling
     * ext_crypto_start_batch_verify. The result of the verification is returned by ext_crypto_finish_batch_verify.
     * If batch verification is not enabled, the signature is verified immediately.
     *
     * @param signature a pointer to the buffer containing the 64-byte signature.
     * @param message   a pointer-size to the message that is to be verified.
     * @param publicKey a pointer to the buffer containing the 256-bit public key.
     * @return an i32 integer value equal to 1 if the signature is valid or batched or a value equal 0 to if otherwise.
     */
    public int ed25519BatchVerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ED25519);

        if (batchVerificationStarted) {
            signaturesToVerify.add(verifySig);
            return 1;
        } else {
            return Ed25519Utils.verifySignature(verifySig) ? 1 : 0;
        }
    }

    /**
     * Returns all sr25519 public keys for the given key identifier from the keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @return a pointer-size to the SCALE encoded array of 256bit public keys.
     */
    public RuntimePointerSize sr25519PublicKeysV1(int keyTypeId) {
        byte[] keyTypeBytes = runtime.getDataFromMemory(new RuntimePointerSize(keyTypeId, KeyType.KEY_TYPE_LEN));
        final KeyType keyType = KeyType.getByBytes(keyTypeBytes);

        if (keyType == null || (keyType.getKey() != Key.SR25519 && keyType.getKey() != Key.GENERIC)) {
            throw new InvalidKeyTypeException(
                    String.format(TYPE_RECEIVED_STRING, keyType != null ? keyType.getKey() : null ));
        }

        return internalPublicKeys(keyType);
    }

    /**
     * Generates an sr25519 key for the given key type using an optional BIP-39 seed and stores it in the keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @param seed      a pointer-size to the SCALE encoded Option value containing the BIP-39 seed which must be valid
     *                  UTF8.
     * @return a pointer to the buffer containing the 256-bit public key.
     * @throws InvalidSeedException Panics if the key cannot be generated, such as when an invalid key type or invalid
     *         seed was provided.
     */
    public int sr25519GenerateV1(int keyTypeId, RuntimePointerSize seed) {
        var pair = getSeedStringAndKeyType(keyTypeId, seed);
        final Schnorrkel.KeyPair keyPair;

        if (pair.getSeed() != null) {
            if (!MnemonicUtils.validateMnemonic(pair.getSeed())) {
                Util.nativePanic(SEED_IS_INVALID);
                throw new InvalidSeedException();
            }
            keyPair = Sr25519Utils.generateKeyPair(pair.getSeed());
        } else {
            keyPair = Sr25519Utils.generateKeyPair();
        }

        keyStore.put(pair.getKeyType(), keyPair.getPublicKey(), keyPair.getSecretKey());
        return runtime.writeDataToMemory(keyPair.getPublicKey()).pointer();
    }

    /**
     * Signs the given message with the sr25519 key that corresponds to the given public key and key type in the
     * keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @param publicKey a pointer to the buffer containing the 256 bit public key.
     * @param message   a pointer-size to the message that is to be signed.
     * @return a pointer-size to the SCALE encoded Option value containing the 64-byte signature.
     * This function returns if the public key cannot be found in the key store.
     */
    public int sr25519SignV1(int keyTypeId, int publicKey, RuntimePointerSize message) {
        final Signature sig = internalGetSignData(keyTypeId, publicKey, message, Key.SR25519);

        byte[] signed = null;
        if (sig.getPrivateKey() != null) {
            signed = Sr25519Utils.signMessage(sig.getPublicKeyData(), sig.getPrivateKey(), sig.getMessageData());
        }

        return runtime.writeDataToMemory(scaleEncodedOption(signed)).pointer();
    }

    /**
     * Verifies an sr25519 signature.
     *
     * @param signature a pointer to the buffer containing the 64-byte signature.
     * @param message   a pointer-size to the message that is to be verified.
     * @param publicKey a pointer to the buffer containing the 256-bit public key.
     * @return a i32 integer value equal to 1 if the signature is valid or a value equal to 0 if otherwise.
     */
    public int sr25519VerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        VerifySignature verifiedSignature = internalGetVerifySignature(signature, message, publicKey, Key.SR25519);

        return Sr25519Utils.verifySignature(verifiedSignature) ? 1 : 0;
    }

    /**
     * Registers a sr25519 signature for batch verification. Batch verification is enabled by calling
     * ext_crypto_start_batch_verify. The result of the verification is returned by ext_crypto_finish_batch_verify.
     * If batch verification is not enabled, the signature is verified immediately.
     *
     * @param signature a pointer to the buffer containing the 64-byte signature.
     * @param message   a pointer-size to the message that is to be verified.
     * @param publicKey a pointer to the buffer containing the 256-bit public key.
     * @return an i32 integer value equal to 1 if the signature is valid or batched or a value equal 0 to if otherwise.
     */
    public int sr25519BatchVerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        VerifySignature verifiedSignature = internalGetVerifySignature(signature, message, publicKey, Key.SR25519);

        if (batchVerificationStarted) {
            signaturesToVerify.add(verifiedSignature);
        } else {
            return Sr25519Utils.verifySignature(verifiedSignature) ? 1 : 0;
        }

        return 1;
    }

    /**
     * Returns all ecdsa public keys for the given key identifier from the keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @return a pointer-size to the SCALE encoded array of 33byte compressed public keys.
     */
    public RuntimePointerSize ecdsaPublicKeysV1(int keyTypeId) {
        byte[] keyTypeBytes = runtime.getDataFromMemory(new RuntimePointerSize(keyTypeId, KeyType.KEY_TYPE_LEN));
        final KeyType keyType = KeyType.getByBytes(keyTypeBytes);

        if (keyType == null || keyType.getKey() != Key.GENERIC) {
            throw new InvalidKeyTypeException(
                    String.format(TYPE_RECEIVED_STRING, keyType != null ? keyType.getKey() : null ));
        }

        return internalPublicKeys(keyType);
    }

    private RuntimePointerSize internalPublicKeys(KeyType keyType) {
        List<byte[]> publicKeys = keyStore.getPublicKeysByKeyType(keyType);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ScaleCodecWriter scaleWriter = new ScaleCodecWriter(baos)) {

            ListWriter<byte[]> listWriter = new ListWriter<>(ScaleCodecWriter::writeByteArray);
            listWriter.write(scaleWriter, publicKeys);
            return runtime.writeDataToMemory(baos.toByteArray());

        } catch (IOException e) {
            throw new ScaleEncodingException("Error while SCALE encoding public keys");
        }
    }

    /**
     * Generates an ecdsa key for the given key type using an optional BIP-39 seed and stores it in the keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @param seed      a pointer-size to the SCALE encoded Option value containing the BIP-39 seed which must be valid
     *                  UTF8.
     * @return a pointer to the buffer containing the 33-byte public key.
     * @throws InvalidSeedException Panics if the key cannot be generated, such as when an invalid key type or invalid
     *         seed was provided.
     */
    public int ecdsaGenerateV1(int keyTypeId, RuntimePointerSize seed) {
        var pair = getSeedStringAndKeyType(keyTypeId, seed);

        final Pair<PrivKey, PubKey> keyPair;
        if (pair.getSeed() != null) {
            if (!MnemonicUtils.validateMnemonic(pair.getSeed())) {
                Util.nativePanic(SEED_IS_INVALID);
                throw new InvalidSeedException();
            }
            keyPair = EcdsaUtils.generateKeyPair(pair.getSeed());
        } else {
            keyPair = EcdsaUtils.generateKeyPair();
        }

        keyStore.put(pair.getKeyType(), keyPair.getSecond().raw(), keyPair.getFirst().raw());
        return runtime.writeDataToMemory(keyPair.getSecond().raw()).pointer();
    }

    /**
     * Signs the given message with the ecdsa key that corresponds to the given public key and key type in the
     * keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @param publicKey a pointer to the buffer containing the 33 bytes public key.
     * @param message   a pointer-size to the message that is to be signed.
     * @return a pointer-size to the SCALE encoded Option value containing the signature.
     * The signature is 65-bytes in size, where the first 512-bits represent the signature and the other 8 bits
     * represent the recovery ID. This function returns if the public key cannot be found in the key store.
     */
    public int ecdsaSignV1(int keyTypeId, int publicKey, RuntimePointerSize message) {
        final Signature sig = internalGetSignData(keyTypeId, publicKey, message, Key.ECDSA);
        sig.setMessageData(HashUtils.hashWithBlake2b(sig.getMessageData()));

        byte[] signed = null;
        if (sig.getPrivateKey() != null) {
            signed = EcdsaUtils.signMessage(sig.getPrivateKey(), sig.getMessageData());
        }

        return runtime.writeDataToMemory(scaleEncodedOption(signed)).pointer();
    }

    /**
     * Signs the prehashed message with the ecdsa key that corresponds to the given public key and key type
     * in the keystore.
     *
     * @param keyTypeId a pointer to the key type identifier.
     * @param publicKey a pointer to the buffer containing the 33 bytes public key.
     * @param message   a pointer-size to the message that is to be signed.
     * @return a pointer-size to the SCALE encoded Option value containing the signature.
     * The signature is 65-bytes in size, where the first 512-bits represent the signature and the other 8 bits
     * represent the recovery ID. This function returns if the public key cannot be found in the key store.
     */
    public int ecdsaSignPrehashedV1(int keyTypeId, int publicKey, RuntimePointerSize message) {
        final Signature sig = internalGetSignData(keyTypeId, publicKey, message, Key.ECDSA);

        byte[] signed = null;
        if (sig.getPrivateKey() != null) {
            signed = EcdsaUtils.signMessage(sig.getPrivateKey(), sig.getMessageData());
        }

        return runtime.writeDataToMemory(scaleEncodedOption(signed)).pointer();
    }

    /**
     * Verifies the hash of the given message against an ECDSA signature.
     *
     * @param signature a pointer to the buffer containing the 65-byte signature. The signature is 65-bytes in size,
     *                  where the first 512-bits represent the signature and the other 8 bits represent the recovery ID.
     * @param message   a pointer-size to the message that is to be verified.
     * @param publicKey a pointer to the buffer containing the 33-byte compressed public key.
     * @return a i32 integer value equal 1 to if the signature is valid or a value equal to 0 if otherwise.
     */
    public int ecdsaVerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        final VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ECDSA);
        verifySig.setMessageData(HashUtils.hashWithBlake2b(verifySig.getMessageData()));
        return EcdsaUtils.verifySignature(verifySig) ? 1 : 0;
    }

    /**
     * Verifies the prehashed message against a ECDSA signature.
     *
     * @param signature a pointer to the buffer containing the 65-byte signature. The signature is 65-bytes in size,
     *                  where the first 512-bits represent the signature and the other 8 bits represent the recovery ID.
     * @param message   a pointer to the 32-bit prehashed message to be verified.
     * @param publicKey a pointer to the buffer containing the 33-byte compressed public key.
     * @return a i32 integer value equal 1 to if the signature is valid or a value equal to 0 if otherwise.
     */
    public int ecdsaVerifyPrehashedV1(int signature, int message, int publicKey) {
        final byte[] signatureData = runtime.getDataFromMemory(new RuntimePointerSize(signature, SIGNATURE_LEN));
        final byte[] messageData = runtime.getDataFromMemory(
                new RuntimePointerSize(message, EcdsaUtils.HASHED_MESSAGE_LEN));
        final byte[] publicKeyData = runtime.getDataFromMemory(
                new RuntimePointerSize(publicKey, EcdsaUtils.PUBLIC_KEY_COMPRESSED_LEN));

        VerifySignature verifySig = new VerifySignature(signatureData, messageData, publicKeyData, Key.ECDSA);
        return EcdsaUtils.verifySignature(verifySig) ? 1 : 0;
    }

    /**
     * Registers ECDSA sr25519 signature for batch verification. Batch verification is enabled by calling
     * ext_crypto_start_batch_verify. The result of the verification is returned by ext_crypto_finish_batch_verify.
     * If batch verification is not enabled, the signature is verified immediately.
     *
     * @param signature a pointer to the buffer containing the 64-byte signature.
     * @param message   a pointer-size to the message that is to be verified.
     * @param publicKey a pointer to the buffer containing the 256-bit public key.
     * @return an i32 integer value equal to 1 if the signature is valid or batched or a value equal 0 to if otherwise.
     */
    public int ecdsaBatchVerifyV1(int signature, RuntimePointerSize message, int publicKey) {
        final VerifySignature verifySig = internalGetVerifySignature(signature, message, publicKey, Key.ECDSA);
        verifySig.setMessageData(HashUtils.hashWithBlake2b(verifySig.getMessageData()));

        if (batchVerificationStarted) {
            signaturesToVerify.add(verifySig);
        } else {
            return EcdsaUtils.verifySignature(verifySig) ? 1 : 0;
        }
        return 1;
    }

    /**
     * Verify and recover a secp256k1 ECDSA signature.
     *
     * @param signature a pointer to the buffer containing the 65-byte signature in RSV format. V should be either
     *                  0/1 or 27/28.
     * @param message   a pointer to the buffer containing the 256-bit Blake2 hash of the message.
     * @return a pointer-size to the SCALE encoded Result. On success it contains the 64-byte recovered public key or
     * an error type on failure.
     */
    public int secp256k1EcdsaRecoverV1(int signature, int message) {
        byte[] ecdsaPublicKey = internalSecp256k1RecoverKey(signature, message, false);
        return secp2561kScaleKeyResult(ecdsaPublicKey);
    }

    /**
     * Verify and recover a secp256k1 ECDSA signature.
     *
     * @param signature a pointer to the buffer containing the 65-byte signature in RSV format. V should be either
     *                  0/1 or 27/28.
     * @param message   a pointer to the buffer containing the 256-bit Blake2 hash of the message.
     * @return a pointer-size (Definition 201) to the SCALE encoded Result value. On success it contains the 33-byte
     * recovered public key in compressed form on success or an error type on failure.
     */
    public int secp256k1EcdsaRecoverCompressedV1(int signature, int message) {
        byte[] rawBytes = internalSecp256k1RecoverKey(signature, message, true);
        return secp2561kScaleKeyResult(rawBytes);
    }

    private int secp2561kScaleKeyResult(byte[] rawBytes) {
        ResultWriter resultWriter = new ResultWriter();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ScaleCodecWriter scaleCodecWriter = new ScaleCodecWriter(baos)) {

            resultWriter.writeResult(scaleCodecWriter, true);
            resultWriter.write(scaleCodecWriter, rawBytes);
            return runtime.writeDataToMemory(baos.toByteArray()).pointer();
        } catch (IOException e) {
            throw new ScaleEncodingException(SCALE_ENCODING_SIGNED_MESSAGE_ERROR);
        }
    }

    private byte[] internalSecp256k1RecoverKey(int signature, int message, boolean compressed) {
        final byte[] messageData = runtime.getDataFromMemory(
                new RuntimePointerSize(message, EcdsaUtils.HASHED_MESSAGE_LEN));
        final byte[] signatureData = runtime.getDataFromMemory(
                new RuntimePointerSize(signature, EcdsaUtils.SIGNATURE_LEN));

        return EcdsaUtils.recoverPublicKeyFromSignature(signatureData, messageData, compressed);
    }

    /**
     * Starts the verification extension. The extension is a separate background process and is used to parallel-verify
     * signatures which are pushed to the batch with ext_crypto_ed25519_batch_verify, ext_crypto_sr25519_batch_verify
     * or ext_crypto_ecdsa_batch_verify. Verification will start immediately and the Runtime can retrieve the result
     * when calling ext_crypto_finish_batch_verify.
     */
    public void startBatchVerify() {
        batchVerificationStarted = true;
    }

    /**
     * Finish verifying the batch of signatures since the last call to this function. Blocks until all the signatures
     * are verified.
     *
     * @return an i32 integer value equal to 1 if all the signatures are valid or a value equal to 0 if one or more of
     * the signatures are invalid.
     */
    public int finishBatchVerify() {
        if (!batchVerificationStarted) {
            Util.nativePanic(BATCH_VERIFICATION_NOT_STARTED);
            throw new BatchVerificationNotStartedException();
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

    private byte[] scaleEncodedOption(byte[] data) {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeOptional(ScaleCodecWriter::writeByteArray, data);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new ScaleEncodingException(SCALE_ENCODING_SIGNED_MESSAGE_ERROR);
        }
    }

}
