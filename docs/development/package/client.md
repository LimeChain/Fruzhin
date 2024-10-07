# Overview

The `client` package of the Fruzhin project encompasses the core components for initiating and managing different types of blockchain nodes. This package is crucial for defining the operational behavior of full nodes and light clients within the network.

## HostNode Interface

The `HostNode` interface establishes the foundational contract for node operations within the Fruzhin blockchain framework. It defines a `start` method, which is essential for initializing the node's functionalities and dependencies.

### Key Responsibilities:

- **Node Initialization**: Ensures that all necessary services and dependencies are correctly instantiated and configured upon the node startup.

## FullNode Class

The `FullNode` class implements the `HostNode` interface, representing a fully operational blockchain node capable of performing all network functions, including transaction processing, block validation, and state storage.

### Core Functionalities:

- **State Initialization**: Initializes the blockchain state, either by loading from an existing database or starting anew from the genesis block.
- **Network Services**: Manages network connections and peer discovery to ensure the node is well-integrated into the blockchain network.
- **Sync Mechanisms**: Depending on the synchronization mode (`FULL` or `WARP`), it initiates the appropriate syncing process to maintain an up-to-date blockchain state.

## LightClient Class

The `LightClient` also implements the `HostNode` interface, focusing on providing a lightweight version of the node that does not require full blockchain data to operate. It is optimized for environments with limited resources.

### Core Functionalities:

- **Network Connectivity**: Establishes connections to the network, facilitating peer discovery and interaction without the need for full blockchain data.
- **Warp Sync**: Employs the `WarpSyncMachine` for efficient state synchronization, allowing the light client to stay updated with the latest blockchain state with minimal data.
