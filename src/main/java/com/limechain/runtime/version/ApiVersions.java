package com.limechain.runtime.version;

import com.limechain.runtime.version.scale.ApiVersionReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ApiVersions {
    private final List<ApiVersion> entries;

    private ApiVersions(List<ApiVersion> entries) {
        this.entries = entries;
    }

    /**
     * Constructs a new ApiVersions from an unmodifiable view of the passed list
     */
    public static ApiVersions of(List<ApiVersion> apiVersions) {
        return new ApiVersions(Collections.unmodifiableList(apiVersions));
    }

    public static ApiVersions decodeNoLength(byte[] scaleEncoded) {
        ScaleCodecReader reader = new ScaleCodecReader(scaleEncoded);
        ScaleReader<ApiVersion> apiVersionReader = new ApiVersionReader();
        List<ApiVersion> apiVersionsRaw = new LinkedList<>();

        //TODO: Think about checking if the length is divisible by the fixed byte size of an encoded api version
        while (reader.hasNext()) {
            try {
                apiVersionsRaw.add(reader.read(apiVersionReader));
            } catch (RuntimeException e) {
                break; // Presume end of iteration (all is ok), although other exceptions are possible
            }
        }

        return ApiVersions.of(apiVersionsRaw);
    }

    public BigInteger getApiVersion(byte[] nameHash) {
        return this.entries.stream()
            .filter(apiVersion ->
                Arrays.equals(nameHash, apiVersion.nameHash()))
            .map(ApiVersion::version)
            .findAny()
            .orElse(BigInteger.valueOf(-1)); // TODO: do we really want ro return -1?
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiVersions that = (ApiVersions) o;
        return Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }
}
