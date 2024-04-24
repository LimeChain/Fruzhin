# Overview

The `chain` package in the Fruzhin project is foundational to understanding and interacting with various blockchain networks. It includes definitions and services crucial for specifying the blockchain network and interacting with its genesis information.

## Chain.java

The `Chain` enum defines supported blockchain networks within the Fruzhin project, making it straightforward to reference these networks throughout the codebase.

### Supported Networks

- **POLKADOT**: Represents the Polkadot network.
- **KUSAMA**: Represents the Kusama network.
- **LOCAL**: A network setting for local development and testing.
- **WESTEND**: Represents the Westend test network.

### Key Features

- Provides a simple method to map string values to specific enum constants, facilitating easy parsing of network names from configurations or command-line arguments.
- Enhances code readability and maintainability by using enum constants instead of hard-coded strings.

## ChainService.java

The `ChainService` class is instrumental in loading, caching, and providing access to chain specification (genesis) information, which is essential for initializing and operating a blockchain node.

### Initialization and Caching

Upon instantiation, `ChainService` attempts to load the chain specification from a cached version in the database. If not available or if operating in a local chain mode (development mode), it loads the specification from a JSON file.

### Development Mode Considerations

- In development mode (`isLocalChain`), the service dynamically loads the local chain's genesis file to accommodate frequent updates.
- For non-local chains, it caches the loaded chain specification in the database for efficient retrieval.

### Chain Live Status

- `isChainLive` method: Determines whether the configured chain is live based on the chain type specified in the chain specification.