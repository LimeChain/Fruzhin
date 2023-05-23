package com.limechain.merkle;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.extern.java.Log;
import ove.crypto.digest.Blake2b;

import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

//! A trie proof is a proof that a certain key in the trie has a certain storage value (or lacks
//! a storage value). The proof can be verified by knowing only the Merkle value of the root node.
//!
//! # Details
//!
//! > **Note**: For reminder, the Merkle value of a node is the hash of its node value, or the
//! >           node value directly if its length is smaller than 32 bytes.
//!
//! A trie proof consists in a list of node values of nodes in the trie. For the proof to be valid,
//! the hash of one of these node values must match the expected trie root node value. Since a
//! node value contains the Merkle values of the children of the node, it is possible to iterate
//! down the hierarchy of nodes until the one closest to the desired key is found.
@Log
public class MerkleProver {
    public static HashMap<Hash256, Integer> decodeAndVerifyProof(byte[] proofs, Hash256 stateRoot) {
        Proof[] decoded = new ListReader<>(new ProofReader())
                .read(new ScaleCodecReader(proofs))
                .toArray(Proof[]::new);
        Arrays.stream(decoded).peek(p -> log.info("Proof: " + new Hash256(p.proof)));
        HashMap<Hash256, Integer> merkeValues = new HashMap<>();
        for (int i = 0; i < decoded.length; i++) {
            var proofEntry = decoded[i].proof;
            byte[] hashBytes = Blake2b.Digest.newInstance(32).digest(proofEntry);
            Hash256 hash = new Hash256(hashBytes);
            log.info("Hash: " + hash);
            merkeValues.put(hash, i);

            // TODO: Implement from here
            // https://github.com/smol-dot/smoldot/blob/7a736b124a0a897d0daa09f68f96c277e146f424/lib/src/trie/proof_decode.rs#L66
            // Probably won't work the rust way so it's better to look at Gossamer...
            var proofEntryOffset = 0;
            if (proofEntry.length == 0) {
                proofEntryOffset = 0;
            } else {

            }

        }

        if (merkeValues.values().size() != decoded.length) {
            throw new RuntimeException("Duplicate hashes in proof");
        }

        if (merkeValues.values().size() == 0) {
            return new DecodedTrieProof(proofs, new TreeMap<>());
        }

        // Keep track of the proof entries that haven't been visited when traversing.
        var unvisitedValues = merkeValues.keySet();

        // Find the expected trie root in the proof. This is the starting point of the verification.
        if (!merkeValues.containsKey(stateRoot)) {
            throw new RuntimeException("State root not found in proofs");
        }
        unvisitedValues.remove(stateRoot);


        return merkeValues;
    }
}
