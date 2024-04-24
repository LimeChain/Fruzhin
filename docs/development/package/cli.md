# CLI Package Documentation

The `cli` package in the Fruzhin project provides a robust framework for parsing and handling command-line arguments. It allows the configuration of node behaviors such as network settings, database paths, synchronization modes, and more through the command line.

## Overview

This package is designed to enhance user interaction with the Fruzhin blockchain node, offering flexibility in the node's operation without altering the codebase. It's essential for configuring the node's operational parameters.

## Components

### Cli.java

The `Cli` class serves as the core component for handling command-line inputs. It defines a set of options that users can specify when launching the Fruzhin node and parses these options to configure the node accordingly.

#### Key Features:

- **Network Configuration**: Allows users to specify the blockchain network (e.g., mainnet, testnet).
- **Database Path**: Users can define a custom path for the node's database.
- **Node Key**: Enables the setting of a node key for network communication.
- **RPC Configuration**: Users can configure RPC settings, including public RPC access and the methods exposed via RPC.
- **Synchronization Mode**: Allows specifying the synchronization mode (e.g., full sync, warp sync) for the node.
- **Prometheus Port**: Configures the port for Prometheus metrics export.

### CliArguments.java

The `CliArguments` record encapsulates the results of parsing command-line arguments. It holds configurations such as the network name, database path, synchronization mode, and RPC settings derived from the command-line inputs.

## Usage

The CLI package is used during the initialization of the Fruzhin node to parse command-line arguments and apply the specified configurations. It abstracts the complexity of command-line parsing and ensures that the node is configured as intended by the user or default values.

Example command:

```bash
java -jar fruzhin.jar --network mainnet --db-path /path/to/db --sync-mode warp --rpc-methods unsafe
```

This command would start a Fruzhin node on the mainnet network, using a custom database path, with warp synchronization mode and exposing all RPC methods, including potentially unsafe ones.

