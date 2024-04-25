### Overview
The `runtime` package is central to the Fruzhin project's ability to execute WebAssembly (WASM) modules. This package facilitates everything from the instantiation of WASM modules, managing memory, handling host functions, to providing specific blockchain-related operations through a runtime environment.

### Main Components
- **`Runtime.java`**: Manages the execution of WASM modules, including instantiation, function calls, and memory management.
- **`RuntimeBuilder.java`**: Constructs runtime instances by setting up necessary configurations and dependencies, including the allocator and host API functions.
- **`WasmCustomSection.java`**: Handles custom sections within WASM modules, often used for embedding additional metadata or configuration.
- **`WasmSectionUtils.java`**: Provides utility functions for parsing and managing various sections of a WASM module.

### Allocator Subpackage
- **`AllocationError.java`**, **`AllocationStats.java`**, **`FreeingBumpHeapAllocator.java`**, **`Header.java`**, **`Order.java`**: These files manage memory allocation within the WASM runtime, employing different strategies and maintaining allocation statistics and error handling.

### Host API Subpackage
- **`AllocatorHostFunctions.java`**, **`ChildStorageHostFunctions.java`**, **`CryptoHostFunctions.java`**, **`HashingHostFunctions.java`**, **`MiscellaneousHostFunctions.java`**, **`OffchainHostFunctions.java`**, **`StorageHostFunctions.java`**, **`TrieHostFunctions.java`**: Implements host API functions that provide the runtime with access to blockchain-specific functionalities like storage, cryptography, off-chain operations, and more.
- **`HostApi.java`**: Central interface that aggregates all host functions and integrates them into the WASM environment.
- **`WasmExports.java`**: Enumerates all exported WASM functions, facilitating interaction between the host system and the WASM module.

### DTO Subpackage (Data Transfer Objects)
- Includes several classes like **`HttpErrorType.java`**, **`HttpStatusCode.java`**, **`InvalidRequestId.java`**, **`Key.java`**, **`RuntimePointerSize.java`**, **`Signature.java`**, **`VerifySignature.java`**: These classes define various data structures used by host functions to interact efficiently with the WASM runtime.

### Versioning Subpackage
- **`ApiVersion.java`**, **`ApiVersions.java`**, **`RuntimeVersion.java`**, **`StateVersion.java`**: Manages versioning of the runtime API, which is crucial for ensuring compatibility between different components of the blockchain network and the runtime environment.
- **`ApiVersionReader.java`**, **`RuntimeVersionReader.java`**: Utilities for reading and interpreting version data embedded in WASM modules.
