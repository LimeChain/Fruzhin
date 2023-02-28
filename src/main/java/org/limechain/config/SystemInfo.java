package org.limechain.config;

import java.util.Properties;

public class SystemInfo extends Config {
    public final String hostName;
    public final String hostVersion;
    public final String role;

    public SystemInfo () {
        Properties properties = this.readConfig();

        // Map host name and version
        // TODO: This could throw an error if the props aren't defined in the config
        this.hostName = properties.get("HOST_NAME").toString();
        this.hostVersion = properties.get("HOST_VERSION").toString();

        // TODO: In the future this will be set depending on CLI params
        this.role = "LightClient";

    }
}
