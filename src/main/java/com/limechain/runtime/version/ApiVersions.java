package com.limechain.runtime.version;

import com.limechain.runtime.version.scale.ApiVersionReader;
import com.limechain.exception.scale.ScaleDecodingException;
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

    /**
     * Scale decodes the list of {@link ApiVersion} from the content of the wasm custom section.
     * The list length is presumed to be missing.
     * @param scaleEncoded the scale encoded list of api versions
     * @return the decoded ApiVersions instance
     * @implNote
     *  for some reason, the "api_versions" wasm custom section's scale encoded content doesn't contain
     *  the length of the list (i.e. number of api versions), so this method tries to decode the list
     *  without knowing its size. In this sense, the term "scale encoded list" should be taken with this note -
     *  its length is not present.
     *  <br>
     *  Source of this conclusion: <a href="https://github.com/smol-dot/smoldot/blob/21be5a1abaebeaf7270a744485b4551da8636fb1/lib/src/executor/host/runtime_version.rs#L297">Smoldot</a>
     */
    public static ApiVersions decodeNoLength(byte[] scaleEncoded) {
        ScaleCodecReader reader = new ScaleCodecReader(scaleEncoded);
        ScaleReader<ApiVersion> apiVersionReader = new ApiVersionReader();
        List<ApiVersion> apiVersionsRaw = new LinkedList<>();

        //NOTE:
        // Instead of relying on an exception, we could check whether the length of the input parameter
        // is divisible by the fixed byte size of an encoded api version
        while (reader.hasNext()) {
            try {
                apiVersionsRaw.add(reader.read(apiVersionReader));
            } catch (RuntimeException e) {
                throw new ScaleDecodingException("Unexpected length mismatch while decoding api versions.", e);
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
