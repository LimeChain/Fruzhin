## FullSync

### Overview
The `fullsync` subpackage is responsible for managing the full synchronization process, ensuring that nodes can download and verify the entire blockchain from the genesis block to the latest block.

### Components

#### FullSyncMachine
- **Purpose**: Coordinates the steps involved in a full blockchain synchronization.
- **Functionality**: Initiates block requests, processes received blocks, and handles the transition between different synchronization states.

## WarpSync

### Overview
`warpsync` focuses on speeding up the synchronization process through a mechanism known as Warp Sync, which involves downloading compressed snapshots of the blockchain state.

### Components

#### WarpSyncMachine
- **Purpose**: Manages the warp sync process, which allows nodes to quickly catch up to the current blockchain state without downloading all individual blocks.
- **Functionality**: Orchestrates the downloading of state fragments, verification of data integrity, and integration of the state into the node’s local blockchain.

#### SyncedState
- **Purpose**: Maintains a shared state across the warp sync process, storing critical information such as the current sync status and received data fragments.
- **Functionality**: Ensures that all components involved in the warp sync process can access and modify the sync state coherently.