package com.limechain.storage.block.map;

import com.limechain.runtime.Runtime;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor
@Log
public class HashToRuntime {
    private final Map<byte[], Runtime> mapping = new HashMap<>();

    /**
     * Gets the runtime instance for a given block hash
     * @param hash block hash
     * @return runtime instance
     */
    public Runtime get(byte[] hash) {
        return mapping.get(hash);
    }

    /**
     * Stores a runtime instance for a given block hash
     * @param hash block hash
     * @param instance runtime instance to store
     */
    public void set(byte[] hash, Runtime instance) {
        mapping.put(hash, instance);
    }

    /**
     * Deletes a runtime instance for a given block hash
     * @param hash block hash
     */
    public void delete(byte[] hash) {
        mapping.remove(hash);
    }

    /**
     * Handles pruning and recording on block finalisation
     *
     * @param newCanonicalBlockHashes the block hashes of the blocks newly finalized
     *                                The last element is the finalized block hash
     */
    public void onFinalisation(List<byte[]> newCanonicalBlockHashes) {
        if (mapping.isEmpty()) {
            log.warning("No runtimes in the mapping");
            return;
        }

        final byte[] finalizedHash = newCanonicalBlockHashes.get(newCanonicalBlockHashes.size() - 1);

        // If there's only one runtime in the mapping, update its key.
        if (mapping.size() == 1) {
            Runtime uniqueAvailableInstance = mapping.values().iterator().next();
            mapping.clear();
            mapping.put(finalizedHash, uniqueAvailableInstance);
            return;
        }

        // Proceed from the end of newCanonicalBlockHashes since the last element is the finalized one.
        // The goal is to find a runtime instance closest to the finalized hash.
        int lastElementIdx = newCanonicalBlockHashes.size() - 1;
        for (int idx = lastElementIdx; idx >= 0; idx--) {
            byte[] currentHash = newCanonicalBlockHashes.get(idx);
            Runtime inMemoryRuntime = mapping.get(currentHash);

            if (inMemoryRuntime != null) {
                // Stop all the running instances created by forks, keeping only the closest instance to the finalized
                // block hash.
                Set<Runtime> stoppedRuntimes = new HashSet<>();
                for (Runtime runtimeToPrune : mapping.values()) {
                    if (!inMemoryRuntime.equals(runtimeToPrune) && !stoppedRuntimes.contains(runtimeToPrune)) {
                        runtimeToPrune.getInstance().close();
                        stoppedRuntimes.add(runtimeToPrune);
                    }
                }

                mapping.clear();
                mapping.put(finalizedHash, inMemoryRuntime);
                break;
            }
        }
    }
}
