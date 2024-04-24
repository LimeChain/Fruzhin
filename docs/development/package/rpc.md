### `RpcConfig.java`

**Description**: Sets up WebSocket handlers for processing both WebSocket and HTTP requests. Essential for enabling RPC interactions over both communication protocols.

**Features**:
- Configures handler mappings for WebSocket connections.
- Ensures proper routing of HTTP upgrade requests to WebSocket protocol.

---

### `RpcMethods.java`

**Description**: Defines the exposure levels for RPC methods. This classification (AUTO, SAFE, UNSAFE) affects which methods are accessible based on their security implications.

**Details**:
- `AUTO`: Exposes all methods locally, restricts access externally.
- `SAFE`: Only safe, predefined list of methods are exposed.
- `UNSAFE`: All RPC methods are accessible, including potentially unsafe ones.

---

### `CommonConfig.java`

**Description**: Initializes common beans and settings required for the RPC framework, including service exporters and network configurations.

**Key Points**:
- Uses `@Configuration` for bean definitions.
- `@EnableScheduling` enables scheduled task executions.

---

### `SubscriptionName.java`

**Description**: Contains names for subscription-based RPC methods, supporting the pub-sub model over RPC.

**Subscriptions Include**:
- Names like `newHeads`, `logs`, etc., facilitating real-time blockchain event subscriptions.

---

### `UnsafeRpcMethod.java`

**Description**: A custom annotation for marking RPC methods as unsafe, guiding their exposure based on the RPC method exposure level.

**Usage**:
- Applied to RPC method implementations considered to be unsafe.

---

### `RPCMethods.java`

**Description**: Aggregates all separate RPC method interfaces into a single interface due to jsonrpc4j's limitations.

**Interfaces**:
- Extends `SystemRPC`, `SyncRPC`, etc.
- Includes a method to list all available RPC methods.

---

### `RPCMethodsImpl.java`

**Description**: Implements the aggregated RPC methods interface, handling RPC calls for various blockchain functionalities.

**Features**:
- Provides logic for system information, blockchain synchronization, state queries, and more.
5