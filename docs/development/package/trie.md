5# Trie Package Overview - Fruzhin Project

## Introduction
The `trie` package of the Fruzhin project is designed to manage the trie data structure, which is a type of search tree used to store dynamic data sets. Tries are particularly useful in blockchain applications for efficiently storing and querying data.

## Key Components

### Trie Data Structure
- **TrieStructure.java**: Implements the trie structure with methods for inserting, deleting, and retrieving nodes based on keys.
- **TrieNode.java**: Specialized node class for trie operations, encapsulating additional trie-specific logic and properties.

### Encoding and Decoding
- **TrieEncoder.java** and **TrieDecoder.java**: Responsible for encoding and decoding trie nodes to and from a compact binary format. This is crucial for storing trie nodes in a space-efficient manner and for transmitting trie data across the network.
- **TrieLeafDecoder.java** and **TrieBranchDecoder.java**: Specialized decoders for handling specific types of trie nodes, enhancing the efficiency of trie operations.

### Utility Classes
- **Nibbles.java**: A utility class for converting keys between various formats used within the trie, facilitating the manipulation and comparison of keys.