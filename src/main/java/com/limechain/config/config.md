## Overview
The `config` package contains classes that are responsible for the configuration of the system. Currently, it includes the `SystemInfo` class.

## SystemInfo Class
File: `src/main/java/com/limechain/config/SystemInfo.java`

The `SystemInfo` class is a configuration class used to hold and information used by the system rpc methods.

### Fields
- `role`: A string representing the role of the node in the network.
- `chain`: An instance of the `Chain` class.
- `dbPath`: A string representing the path to the database.
- `hostIdentity`: A string representing the identity of the host.
- `hostName`: A string representing the name of the host.
- `hostVersion`: A string representing the version of the host.

### Constructor
The `SystemInfo` constructor initializes the `role`, `chain`, `dbPath`, and `hostIdentity` fields.

### Methods
- `logSystemInfo()`: This method logs system info on node startup.

```java
public class SystemInfo {
    private final String role;
    private final Chain chain;
    private final String dbPath;
    private final String hostIdentity;
    @Value("${host.name}")
    private String hostName;
    @Value("${host.version}")
    private String hostVersion;

    public SystemInfo(HostConfig hostConfig, Network network) {
        this.role = network.getNodeRole().name();
        this.chain = hostConfig.getChain();
        this.dbPath = hostConfig.getRocksDbPath();
        this.hostIdentity = network.getHost().getPeerId().toString();
    }

    public void logSystemInfo() {
        // Implementation details...
    }
}
```