package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class CryptoHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostFunctions.getImportObject("ext_crypto_ed25519_public_keys_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32), Type.I64),
                HostFunctions.getImportObject("ext_crypto_ed25519_generate_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64), Type.I32),
                HostFunctions.getImportObject("ext_crypto_ed25519_sign_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostFunctions.getImportObject("ext_crypto_ed25519_verify_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_ed25519_batch_verify_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_sr25519_public_keys_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32), Type.I64),
                HostFunctions.getImportObject("ext_crypto_sr25519_generate_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64), Type.I32),
                HostFunctions.getImportObject("ext_crypto_sr25519_sign_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostFunctions.getImportObject("ext_crypto_sr25519_verify_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_sr25519_verify_version_2", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_sr25519_batch_verify_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_ecdsa_public_key_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), (Type.I64)),
                HostFunctions.getImportObject("ext_crypto_ecdsa_generate_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64), (Type.I64)),
                HostFunctions.getImportObject("ext_crypto_ecdsa_sign_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostFunctions.getImportObject("ext_crypto_ecdsa_sign_prehashed_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32, Type.I64), (Type.I64)),
                HostFunctions.getImportObject("ext_crypto_ecdsa_verify_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_ecdsa_verify_version_2", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_ecdsa_verify_prehashed_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_ecdsa_batch_verify_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_crypto_secp256k1_ecdsa_recover_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32), (Type.I64)),
                HostFunctions.getImportObject("ext_crypto_secp256k1_ecdsa_recover_version_2", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32), Type.I64),
                HostFunctions.getImportObject("ext_crypto_secp256k1_ecdsa_recover_compressed_version_1",
                        argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32), Type.I64),
                HostFunctions.getImportObject("ext_crypto_secp256k1_ecdsa_recover_compressed_version_2",
                        argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I32), Type.I64),
                HostFunctions.getImportObject("ext_crypto_start_batch_verify", argv -> {
                    return HostFunctions.EMPTY_LIST_OF_NUMBER;
                }, HostFunctions.EMPTY_LIST_OF_TYPES),
                HostFunctions.getImportObject("ext_crypto_finish_batch_verify", argv -> {
                    return argv;
                }, HostFunctions.EMPTY_LIST_OF_TYPES, Type.I32));
    }

}
