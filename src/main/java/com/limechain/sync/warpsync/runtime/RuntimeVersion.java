package com.limechain.sync.warpsync.runtime;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
public class RuntimeVersion {
    String specName;
    String implementation;
    BigInteger authoringVersion;
    BigInteger specVersion;
    BigInteger implementationVersion;
    BigInteger transactionVersion;
    BigInteger stateVersion;
    RuntimeApis runtimeApis;

    @Override
    public String toString() {
        return "RuntimeVersion:{specName=" + this.specName +
                "; implementation=" + this.implementation +
                "; authoringVersion=" + this.authoringVersion +
                "; specVersion=" + this.specVersion +
                "; implementationVersion=" + this.implementationVersion +
                "; transactionVersion=" + this.transactionVersion +
                "; stateVersion=" + this.stateVersion;
    }

    public void decode(ScaleCodecReader reader) {
        this.setSpecName(reader.readString());
        this.setImplementation(reader.readString());
        this.setAuthoringVersion(BigInteger.valueOf(reader.readUint32()));
        this.setSpecVersion(BigInteger.valueOf(reader.readUint32()));
        this.setImplementationVersion(BigInteger.valueOf(reader.readUint32()));

        // Probably only reads a 0 byte since the runtime apis have been moved to a different custom sections
        int apiVersionsSize = reader.readCompactInt();
        byte[][] apiVersions = new byte[apiVersionsSize][];
        long[] apiVersionsNumbers = new long[apiVersionsSize];
        for (int i = 0; i < apiVersionsSize; i++) {
            apiVersions[i] = reader.readByteArray(8);
            apiVersionsNumbers[i] = reader.readUint32();
        }

        this.setTransactionVersion(BigInteger.valueOf(reader.readUint32()));
        this.setStateVersion(BigInteger.valueOf(reader.readUByte()));
    }
}
