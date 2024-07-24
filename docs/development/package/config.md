# Overview

The `config` package in the Fruzhin project is pivotal for defining and managing the configuration settings required for the blockchain node's operation. It encompasses the setup of host-specific configurations and system information that influence the node's behavior and interaction within the network.

## HostConfig.java

The `HostConfig` class serves as a central configuration hub, aggregating settings derived from command-line arguments or default values to configure the blockchain host effectively.

### Features and Responsibilities:

- **Database Path Configuration**: Specifies the file path for the node's database, facilitating data persistence.
- **Blockchain Network Selection**: Determines the blockchain network (e.g., Polkadot, Kusama, Westend, Local) the node will connect to and operate within.
- **Node Role Determination**: Defines the role of the node within the network, such as a full node or a light client, influencing its operational scope.
- **RPC Configuration**: Manages RPC node address settings.
- **Dynamic Genesis Path Resolution**: Dynamically resolves the path to the genesis file based on the configured blockchain network, ensuring correct initialization parameters.

### Usage:

`HostConfig` is instantiated with `CliArguments`, ensuring all command-line configurations are reflected in the host's operational settings. It plays a crucial role in initializing and tailoring the node's environment to meet specified requirements or preferences.

## SystemInfo.java

The `SystemInfo` class encapsulates various system-related configurations and information, facilitating the retrieval of node and environment details.

### Features and Responsibilities:

- **Role and Chain Information**: Stores and provides access to the node's role and the blockchain network it is part of, aiding in contextual operations and decision-making.
- **Database and Identity Information**: Maintains the path to the database and the node's identity within the network, crucial for data storage and peer interactions.
- **Versioning and Host Details**: Keeps track of the node's version and host information, supporting compatibility and operational insights.
