package com.limechain.storage.crypto;

import com.limechain.storage.KVRepository;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import lombok.extern.java.Log;
import org.javatuples.Pair;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Log
public class KeyStore {

    private final KVRepository<String, Object> repository;

    public KeyStore(KVRepository<String, Object> repository) {
        this.repository = repository;
    }

    public void put(KeyType keyType, byte[] publicKey, byte[] privateKey) {
        repository.save(getKey(keyType, publicKey), privateKey);
    }

    public byte[] get(KeyType keyType, byte[] publicKey) {
        return (byte[]) repository.find(getKey(keyType, publicKey)).orElse(null);
    }

    public boolean contains(KeyType keyType, byte[] publicKey) {
        return get(keyType, publicKey) != null;
    }

    /**
     * Retrieves a list of public keys associated with the given key type.
     *
     * @param keyType The type of the key.
     * @return A list of public keys associated with the given key type.
     */
    public List<byte[]> getPublicKeysByKeyType(KeyType keyType) {
        return repository
                .findKeysByPrefix(new String(keyType.getBytes()), 90000)
                .stream()
                .map(this::removeKeyTypeFromKey)
                .toList();
    }

    /**
     * Searches for a public - private key pair within the provided list of authPubKeys
     * and retrieves the corresponding key pair if available.
     *
     * @param authPubKeys the list of authPubKeys to search for the key pair.
     * @param type        the type of cryptographic algorithm used for the key.
     * @return an {@link Optional} containing a {@link Pair} of byte arrays representing the public and private keys,
     * or an empty {@link Optional} if no matching key pair is found.
     */
    public Optional<Pair<byte[], byte[]>> findKeyPair(List<byte[]> authPubKeys, KeyType type) {
        return authPubKeys.stream()
                .map(a -> getKeyPair(type, a))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    /**
     * Retrieves a public - private key pair from the store.
     * SR25519 type for BABE
     * ED25519 type for GRANDPA
     * ECDSA type for BEEFY
     *
     * @param type      the algorithm type that the key is used for.
     * @param publicKey the pubKey used as a key to retrieve a privKey from the store.
     * @return Pair of (publicKey, privateKey)
     */
    public Optional<Pair<byte[], byte[]>> getKeyPair(KeyType type, byte[] publicKey) {
        var privateKey = get(type, publicKey);
        if (privateKey != null) {
            return Optional.of(new Pair<>(publicKey, privateKey));
        }

        return Optional.empty();
    }

    public Schnorrkel.KeyPair convertToSchnorrKeypair(Pair<byte[], byte[]> pair) {
        Schnorrkel.PublicKey pubKey = new Schnorrkel.PublicKey(pair.getValue0());
        return new Schnorrkel.KeyPair(pubKey, pair.getValue1());
    }

    private byte[] removeKeyTypeFromKey(byte[] key) {
        return Arrays.copyOfRange(key, KeyType.KEY_TYPE_LEN, key.length);
    }

    /**
     * Constructs the key using the key type and key bytes.
     *
     * @param keyType The type of the key.
     * @param key     The key bytes.
     * @return The constructed key string.
     */
    private String getKey(KeyType keyType, byte[] key) {
        return new String(keyType.getBytes()).concat(new String(key));
    }
}
