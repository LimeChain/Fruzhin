## Overwier
The `utils` package in the Fruzhin project offers a set of utility classes that support various operations such as data manipulation, hashing, and encoding. These utilities are crucial for the efficient handling of data throughout the blockchain system.

## Key Components

### ByteArrayUtils
- **Purpose**: Provides utility methods for operations on byte arrays, which are extensively used in blockchain for data storage and manipulation.
- **Key Methods**:
    - **commonPrefixLength(byte[] a, byte[] b)**: Calculates the length of the common prefix between two byte arrays, useful for trie operations and data comparisons.
    - **hasPrefix(byte[] array, byte[] prefix)**: Checks if a given byte array starts with a specified prefix, aiding in data validation and routing.
    - **indexOf(byte[] array, byte[] target)**: Finds the first occurrence of a byte sequence within another byte array, useful for parsing and data extraction.
    - **concatenate(byte[] prefix, byte[] suffix)**: Combines two byte arrays into one, commonly used in data processing and block construction.

### HashUtils
- **Purpose**: Facilitates various hashing functions that are essential for security and data integrity in blockchain operations.
- **Key Methods**:
    - **hashWithBlake2b(byte[] input)**: Performs a Blake2b hash on the input, returning a 256-bit hash used for block and transaction identifiers.
    - **hashWithKeccak256(byte[] input)**: Generates a Keccak-256 hash, widely used in Ethereum-like blockchains for hashing states and transactions.
    - **hashWithSha256(byte[] input)**: Provides a SHA-256 hash, standard in many cryptographic operations and data verification processes.

### HexUtils
- **Purpose**: Assists in hexadecimal data conversions that are necessary for encoding and decoding data in the blockchain context.
- **Key Methods**:
    - **encode(byte[] data)**: Converts byte data into a hexadecimal string, useful for readable display and storage.
    - **decode(String hex)**: Transforms a hexadecimal string back into its byte array form, essential for data processing and transactions.
