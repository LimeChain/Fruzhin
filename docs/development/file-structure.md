# File structure


## Genesis Configuration

- **genesis/**: Stores JSON files with genesis block configurations for various networks like Polkadot and Kusama, crucial for initializing the blockchain.

## Libraries

- **libs/**: Hosts external libraries used in the project to support functionalities such as JSON RPC and blockchain interactions.

## Main Source Code

- **src/main/java/com/limechain/**: The core directory of the project, containing the Java source code.
    - **[chain/](package/chain.md)**: Related to the blockchain's chain logic, services, and synchronization.
    - **[cli/](package/cli.md)**: Utilities for the command-line interface to interact with the blockchain network.
    - **[client/](package/client.md)**: Defines different blockchain network clients (e.g., Full Node, Light Client).
    - **[config/](package/config.md)**: Application and network configuration classes.
    - **[constants/](package/constants.md)**: Holds constant values utilized throughout the project.
    - **[exception/](package/exception.md)**: Custom exception classes for blockchain operations and validations.
    - **[network/](package/network.md)**: Manages network connections, protocols, and data transfers.
    - **[rpc/](package/rpc.md)**: Classes for Remote Procedure Call (RPC) communications between nodes.
    - **[runtime/](package/runtime.md)**: Manages the blockchain runtime environment and interactions with WebAssembly (WASM).
    - **[storage/](package/storage.md)**: Data storage and database interaction functionality.
    - **[sync/](package/sync.md)**: Synchronization logic for the blockchain state and transactions.
    - **[trie/](package/trie.md)**: Implementation of the Trie data structure for blockchain state management.
    - **[utils/](package/utils.md)**: Utility classes for hashing, encoding/decoding, and more.

## Tests

- **src/test/java/com/limechain/**: Contains tests for various components of the project to ensure the reliability and correctness of the implementation.

## Miscellaneous

- **gradle/wrapper/**: Gradle wrapper files for a consistent build environment.
- **zombienet/**: The Zombienet tests, required to verify proper node operation.
