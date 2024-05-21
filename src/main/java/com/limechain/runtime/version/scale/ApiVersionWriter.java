package com.limechain.runtime.version.scale;

import com.limechain.runtime.version.ApiVersion;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class ApiVersionWriter implements ScaleWriter<ApiVersion> {
    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, ApiVersion apiVersion) throws IOException {
        scaleCodecWriter.writeByteArray(apiVersion.nameHash());
        scaleCodecWriter.writeUint32(apiVersion.version().longValueExact());
    }
}
