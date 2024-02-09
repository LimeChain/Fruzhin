package com.limechain.runtime.version;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Objects;

// TODO: Remove the need for a public @Setter
//  Ideally, we'd want this class to be a record, but we currently need to partially build it (due to api versions)
//  and this happens in more than one package... think about repackaging
@Getter
@Setter
public class RuntimeVersion {
    private String specName;
    private String implementationName;
    private BigInteger authoringVersion;
    private BigInteger specVersion;
    private BigInteger implementationVersion;


    // TODO: [Naming inconsistency] the spec names this type `ApiVersions`
    //  but it's more semantically a list of rather 'runtime api' data holders
    //  (each of them holding the hashed name of the api and its version (a number))
    /**
     * List of "API"s that the runtime supports.
     * Each API corresponds to a certain list of runtime entry points.
     * This field can thus be used in order to determine which runtime entry points are
     * available.
     */
    private ApiVersions apis;

    /**
     * Arbitrary version number corresponding to the transactions encoding version.
     * Whenever this version number changes, all transactions encoding generated earlier
     * are invalidated and should be regenerated.
     * Older versions of Substrate didn't provide this field.
     * <br>
     * Null if the field is missing.
     */
    @Nullable
    private BigInteger transactionVersion;


    /**
     * Version number of the state trie encoding version.
     * Version 0 corresponds to a different trie encoding than version 1.
     * This field has been added to Substrate on 24th December 2021. Older versions of Substrate
     * didn't provide this field, in which case we fall back to StateVersion 0 (the original old one).
     */
    private StateVersion stateVersion;

    @Override
    public String toString() {
        return "RuntimeVersion:{specName=" + this.specName +
               "; implementationName=" + this.implementationName +
               "; authoringVersion=" + this.authoringVersion +
               "; specVersion=" + this.specVersion +
               "; implementationVersion=" + this.implementationVersion +
               "; transactionVersion=" + this.transactionVersion +
               "; stateVersion=" + this.stateVersion + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuntimeVersion version = (RuntimeVersion) o;
        return Objects.equals(specName, version.specName) &&
               Objects.equals(implementationName, version.implementationName) &&
               Objects.equals(authoringVersion, version.authoringVersion) &&
               Objects.equals(specVersion, version.specVersion) &&
               Objects.equals(implementationVersion, version.implementationVersion) &&
               Objects.equals(apis, version.apis) &&
               Objects.equals(transactionVersion, version.transactionVersion) &&
               stateVersion == version.stateVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(specName, implementationName, authoringVersion, specVersion, implementationVersion, apis,
            transactionVersion, stateVersion);
    }
}
