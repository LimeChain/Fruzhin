package com.limechain.runtime.version;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * A single api version, decoded as an element of the list of api versions within the runtime version
 *
 * @param nameHash - the blake2 hash of length 8 bytes of the name of the API
 * @param version - version of the module. Typical values are `1`, `2`, `3`, ...
 */
public record ApiVersion(byte[] nameHash, BigInteger version) {
    public static final int NAME_HASH_LENGTH = 8;

    /**
     * Compares the names of the apis, thus whether both objects identify the same semantic api,
     * but possibly different versions of it
     * @return true if names equal, false otherwise
     */
    public boolean sameNameAs(ApiVersion other) {
        return Arrays.equals(nameHash, other.nameHash);
    }

    @Override
    public String toString() {
        return "ApiVersion{" +
               "nameHash=" + Arrays.toString(nameHash) +
               ", version=" + version +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiVersion that = (ApiVersion) o;
        return Objects.equals(version, that.version) && Arrays.equals(nameHash, that.nameHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(version);
        result = 31 * result + Arrays.hashCode(nameHash);
        return result;
    }
}
