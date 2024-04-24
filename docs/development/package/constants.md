# Constants Package Documentation

The `constants` package in the Fruzhin project houses essential, immutable values that are pivotal for the operation of the blockchain node. It includes definitions for genesis block information and RPC connection constants.

## GenesisBlockHash.java

`GenesisBlockHash` encapsulates the foundational block information for the blockchain, serving as a cornerstone for the node's operation within a specific network.

### Key Components:

- **genesisHash**: Represents the hash of the genesis block, uniquely identifying the starting point of the blockchain.
- **genesisStorage**: A map containing the storage key-value pairs from the genesis block, essential for initializing the blockchain state.
- **genesisTrie**: A trie structure built from the genesis storage, facilitating efficient data retrieval and state verification.
- **genesisBlockHeader**: Contains metadata for the genesis block, including state root and extrinsics root hashes.

### Functionalities:

- **Genesis Block Construction**: Utilizes the genesis storage to construct a trie structure and generate a block header for the genesis block, incorporating the calculated state root hash.

## RpcConstants.java

`RpcConstants` defines WebSocket RPC endpoints for major Polkadot networks, enabling the node to connect to the blockchain network for synchronization and interaction.

### Defined Constants:

- **POLKADOT_WS_RPC**: WebSocket RPC endpoint for the Polkadot mainnet.
- **KUSAMA_WS_RPC**: WebSocket RPC endpoint for the Kusama network.
- **WESTEND_WS_RPC**: WebSocket RPC endpoint for the Westend testnet.

### Usage:

These constants are used throughout the Fruzhin project to establish connections to the specified blockchain networks, facilitating data fetches, transaction submissions, and other network interactions.
