### Overview

The `exception` package is categorized into several subpackages, each dedicated to a specific aspect of the system, such as chain operations, host API interactions, RPC communications, storage, and more. This categorization helps in precisely handling errors related to different functionalities of the blockchain system.

### Subpackages and Key Exceptions

#### Global Exceptions
- **ExecutionFailedException**: Handles failures during execution processes.
- **MissingObjectException**: Thrown when an expected object is not found.
- **RuntimeCodeException**: Covers exceptions related to runtime code errors.
- **ThreadInterruptedException**: Deals with errors arising from interrupted threads.

#### Host API Exceptions
- **InvalidArgumentException**, **InvalidKeyTypeException**, **InvalidSeedException**: Handle errors related to invalid inputs or configurations in host API calls.
- **OffchainResponseWaitException**, **SocketTimeoutException**: Manage timeouts and response delays in off-chain operations.

#### Chain Exceptions
- **InvalidChainException**: Thrown when interacting with an invalid blockchain configuration.
- **InvalidNodeRoleException**: Handles incorrect node role assignments.
- **Sr25519Exception**: Specific to errors in the Sr25519 cryptographic operations.

#### Network Exceptions
- **SignatureCountMismatchException**: Deals with mismatches in the expected and actual count of signatures in network operations.
- **PeerNotFoundException**: Thrown when a specified peer is not found in the network.

#### RPC Exceptions
- **InvalidParametersException**, **InvalidURIException**: Cover errors related to invalid RPC call parameters or URIs.
- **NotificationFailedException**, **WsMessageSendException**: Handle failures in sending notifications or messages over WebSocket.

#### Storage Exceptions
- **BlockAlreadyExistsException**, **BlockNotFoundException**: Manage issues related to block existence or absence in the storage.
- **DBException**: Generic exception for database-related errors.

#### Synchronization Exceptions
- **BlockExecutionException**: Covers failures in block execution during the synchronization process.
- **JustificationVerificationException**: Deals with issues in verifying justifications during block import.

#### Trie Exceptions
- **InvalidSlabIndexException**, **NodeDecodingException**, **TrieEncoderException**: Handle various issues related to trie data structures, including encoding and decoding errors.