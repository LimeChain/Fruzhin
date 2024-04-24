# Overview

The `protocol` subpackage of the Fruzhin project is dedicated to defining the various network protocols and services that facilitate communication and data exchange within the blockchain network.

## NetworkService.java

Serves as a base class for all network protocol services, providing a common structure for initializing protocol-specific bindings.

### Key Features:

- **Generic Protocol Handling**: Facilitates the creation and management of various protocol services by encapsulating common functionalities.

## BlockAnnounce Protocol

Concerned with announcing new blocks to peers, this protocol ensures all nodes are synchronized with the latest blockchain state.

### Components:

- **BlockAnnounceService**: Manages the sending of block announcement messages and handshakes to peers.
- **NodeRole**: Enumerates the roles a node can play in the network (e.g., FULL, LIGHT, AUTHORING), affecting the data it may need or provide.
- **BlockAnnounceHandshake and Message**: Defines the structure for block announcement handshakes and messages, containing essential block information.

## Grandpa Protocol

Facilitates finality gadget messages for the blockchain, ensuring network consensus on finalized blocks.

### Components:

- **GrandpaService**: Handles sending neighbor messages or handshakes related to the GRANDPA finality gadget.

## Light Client Protocol

Enables light clients to request and receive blockchain data without needing the full blockchain state.

### Components:

- **LightMessagesService**: Manages light client message protocols, allowing light clients to interact with the network efficiently.

## Ping Protocol

Used for network diagnostics and peer latency measurements, ensuring healthy network connections.

### Components:

- **Ping**: Implements ping operations for measuring peer response times and network health.

## Sync Protocol

Manages block and state synchronization across the network, ensuring all nodes maintain an up-to-date view of the blockchain.

### Components:

- **SyncService**: Facilitates synchronization messages between peers, handling block data requests and responses.

## Transactions Protocol

Handles transaction propagation across the network, ensuring transactions are disseminated to all relevant parties.

### Components:

- **TransactionsService**: Manages sending transaction-related messages to peers, supporting network-wide transaction awareness.

## Warp Sync Protocol

Aids in rapidly synchronizing nodes to the latest blockchain state, crucial for nodes that have been offline or are newly joining the network.

### Components:

- **WarpSyncService**: Implements the warp sync protocol, facilitating fast state synchronization for nodes.
