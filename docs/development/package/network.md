# Overview

The `network` package in the Fruzhin project orchestrates all network-related operations, encompassing peer connections, protocol implementations, and communication strategies for the blockchain node.

## ConnectionManager.java

`ConnectionManager` acts as the central hub for managing peer connections, including tracking connected peers, their streams, and the protocols they support.

### Key Features:

- **Peer Management**: Stores information about connected peers and the protocols they support, facilitating easy retrieval and management of peer connections.
- **Stream Management**: Handles opening and closing of protocol streams (e.g., BLOCK_ANNOUNCE, GRANDPA, TRANSACTIONS) for communication with peers.
- **Peer Updates**: Provides functionality to update peer information based on received messages, such as block announcements or handshake messages.

## Network.java

The `Network` class is responsible for initializing and managing the node's network layer, including setting up peer connections, handling protocol initializations, and managing peer discovery through Kademlia DHT.

### Core Functionalities:

- **Protocol Initialization**: Sets up various protocols like block announce, grandpa, sync, transactions, and light client messages.
- **Peer Discovery**: Utilizes the Kademlia DHT for peer discovery, connecting to boot nodes, and maintaining an optimal number of peer connections.
- **Peer Connection Management**: Manages connections to peers, including starting and stopping the network module and handling peer pings.

## ProtocolUtils.java

`ProtocolUtils` provides utility functions for generating protocol IDs for various network protocols, aiding in protocol identification and differentiation.

### Utilities:

- Offers methods to generate protocol IDs for PING, Kademlia DHT, sync, block announce, grandpa, transactions, light messages, and warp sync protocols based on the chain ID.

## StrictProtocolBinding.java

Abstracts the functionality for protocol binding, ensuring strict adherence to protocol specifications during communication, and facilitates dialing peers with specific protocols.

## Dto Subpackage

Contains data transfer objects (DTOs) like `PeerInfo`, `ProtocolStreamType`, and `ProtocolStreams`, which are crucial for representing peer information, protocol types, and protocol-specific streams respectively.

### Key Components:

- **PeerInfo**: Represents detailed information about a peer, including its ID, node role, and blockchain-specific details like best block and genesis block hash.
- **ProtocolStreamType**: Enumerates the types of protocol streams supported (e.g., GRANDPA, BLOCK_ANNOUNCE, TRANSACTIONS).
- **ProtocolStreams**: Manages initiator and responder streams for a given protocol type between peers.

## Encoding Subpackage

Focuses on encoding and decoding of data frames, specifically implementing LEB128 encoding for message length, critical for SCALE (Simple Concatenated Aggregate Little-Endian) encoded data communication in the blockchain network.

### Components:

- **Leb128LengthFrameDecoder**: Decodes LEB128-encoded message lengths from incoming data streams.
- **Leb128LengthFrameEncoder**: Encodes message lengths into LEB128 format for outgoing data streams.

## Kad Subpackage

Contains utilities and services related to Kademlia DHT operations, including peer discovery, connection management, and handling of distributed hash tables.

### Highlights:

- **KademliaService**: Implements the Kademlia distributed hash table protocol for efficient peer discovery and management within the network.

## Protobuf Subpackage

Defines Protobuf schemas for light client and synchronization messages, enabling structured and efficient communication for blockchain state queries and sync operations.
