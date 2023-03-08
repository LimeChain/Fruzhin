package com.limechain.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

@Getter
public class SystemInfo {
    private final String role;
    @Value("host.name")
    private String hostName;
    @Value("host.version")
    private String hostVersion;

    public SystemInfo () {
        // TODO: In the future this will be set depending on CLI params
        this.role = "LightClient";
    }
}
