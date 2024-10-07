## Storage Package
The `storage` package in the Fruzhin project is essential for the efficient management and persistence of blockchain data. It provides robust mechanisms for storing and retrieving all forms of blockchain-related data, including blocks, transactions, and blockchain state.

## Components of the Storage Package

### Block Storage
- **BlockState.java**: Manages the storage and retrieval of blockchain blocks. It ensures that blocks are stored securely and can be accessed efficiently for blockchain operations.

### Trie Storage
- **TrieStorage.java**: Manages operations on trie structures, such as insertion, deletion, and retrieval of nodes. This component ensures that state data is consistent and accurately represents the latest state of the blockchain.

### Key-Value Repository
- **KVRepository.java**: Defines a generic interface for key-value storage operations. This interface abstracts the underlying storage mechanism, allowing for flexible integration with various database systems or custom storage solutions.
