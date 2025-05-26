# Project TODOs

## Existing TODOs
- Add a regression end2end/integration test for `RuntimeBuilder` that actually verifies
  that [falling back to calling `Core_version`](https://github.com/LimeChain/Fruzhin/blob/428d8589b7cf2f429cdedb43243d68ba84f29ecb/src/main/java/com/limechain/runtime/RuntimeBuilder.java#L48)
  on a real runtime wasm blob fetches the correct `RuntimeVersion`
- Think about separating the production resources from integration test resources
  [here](https://github.com/LimeChain/Fruzhin/blob/428d8589b7cf2f429cdedb43243d68ba84f29ecb/src/test/java/com/limechain/chain/StateRootHashesIntegrationTest.java#L17-L21),
  where we're using hardcoded paths of chain specs which could change thus breaking the test. Generally, what's the in-memory entry point of the notion of a "chain", for now, we're using enum-like logic for supporting a finite list of chains, so starting from there could make sense.
  So: map the CLI argument to an ENUM instance and from there:
  - in production, use `application.properties` to fetch the chain spec path;
  - in test, use another properties file (spring profiles could help) or implement local test-case specific map from enum to paths... something like that
- Add exception handling (Error Resolver while creating the JSONRPC server) for RPC calls, as currently we are leaking classpath to the exceptions and not using the suggested error codes from the JSON-RPC spec. Look into (./libs/jsonrpc4j-1.6.2-SNAPSHOT.jar!/com/googlecode/jsonrpc4j/JsonRpcBasicServer.class:468)
- Revisit the conversations on the VRF proofs PR from within PolkaJ's Schnorrkel wrapper [here](https://github.com/LimeChain/polkaj/pull/2). Goal: we're waiting for potential upgrade of `robusta` to a newer jni to refine the ugly hacks in our solution. More details in the PR conversation.
- Make `wasmer-java` load the native libraries as dynamic libs and remove the need for manual intervention for the user from [the README](https://github.com/LimeChain/Fruzhin/blob/dev/README.md#wasmer-java-dylib-setup). In other words, add the mentioned "automation later" :D
  I'd suggest a similar approach to what's in `polkaj-schnorrkel`, still not ideal for the dev, but definitely better for the user.
- Runtime binding to context. For now, this happens on construction of the `Runtime`, which means that we bind to the context at the same time we want to build the `byte[]` source.
  An alternative to that would be to delay the injection of the context, e.g. whenever we make a `call`.
  See the discussion [here](https://github.com/LimeChain/Fruzhin/pull/456#discussion_r1622198967)

## Client and Node TODOs
- [HostNode.java] Find a better way to decide whether we've got any states written in the database
- [AuthoringNode.java] Add babe and grandpa services - currently not added because they don't implement 
HostService interface. Need to refactor the service architecture to properly integrate these consensus services.

## RPC and PubSub TODOs
- [PubSubService.java] Instantiate more subscriber channels in the future if needed
- [JsonRpcResponseResult.java] Add subscription information
- [RpcApp.java] Refactor stop() method to properly shut down all services instead of just closing the Spring context
- [RpcWsHandler.java] Implement cleanup of PubSubService subscribers when WebSocket sessions are closed to prevent memory leaks
- [SyncRPCImpl.java] Consider whether to send non-raw genesis if raw is false
- [SyncRPCImpl.java] Update local genesis with the Trie and save it
- [StateRPCImpl.java] Determine if incoming requests will only ask for state in finalized block
- [StateRPCImpl.java] Fix RPC implementation to properly find state (runtime)

## Sync and State TODOs
- [ChainInformationDownloadAction.java] After runtime is downloaded, download and compute chain information
- [ChainInformationDownloadAction.java] Make runtime calls
- [SyncStateRequesterRpc.java] Consider requesting state for different block instead of aborting
- [RequestFragmentsAction.java] Set error state for next() to transition to correct next state
- [ChainInformation.java] Fill missing calls when runtime calls are working
- [WarpSyncState.java] Fetch heap pages from out storage
- [FullSyncMachine.java] Fix sync improvements for polkadot chain

## Trie and Storage TODOs
- [NibblesCollector.java] Consider useful characteristics
- [TrieChanges.java] Optimize to avoid traversing until end of map if missing
- [BlockHandler.java] Implement handleBeefyConsensusMessage
- [BlockState.java] Discuss what needs to be saved for block state
- [BlockState.java] Implement tries.delete for blockheader.StateRoot
- [BlockState.java] Implement BABE - setFirstSlotOnFinalisation
- [BlockState.java] Implement Trie-related functionality
- [BlockState.java] Handle case when currentFinalizedHash is not equal to subchain hash
- [BlockTree.java] Implement pruning of historical states for finalized blocks
- [DecodedNode.java] Optimize to reduce unnecessary copying
- [TrieStorage.java] Address known issues

## Network and Protocol TODOs
- [PeerMessageCoordinator.java] Implement broadcasting of externally incoming grandpa messages
- [BlockAnnounceEngine.java] Send message to network module
- [BlockAnnounceEngine.java] Send block requests to the peer that announced the block
- [GrandpaService.java] Handle handshakes separately
- [GrandpaEngine.java] Implement reputation lowering for peers
- [BlockHeader.java] Make constants configurable
- [Justification.java] Review handling of ancestryVotes
- [NetworkService.java] Fix bug with empty listenAddresses list
- [NetworkService.java] Fix ping requests being rejected due to timeoutScheduler
- [NetworkService.java] Synchronize with findPeers method

## Consensus TODOs
- [BlockProductionVerifier.java] Make key available before method start to avoid duplicate code
- [GrandpaRound.java] Reverse order by block number
- [GrandpaRound.java] Check if we need to retrieve only weight of equivocator
- [GrandpaRound.java] Use BlockState.getAllDescendants() with limits
- [BeefyState.java] Remove authority set
- [BeefyState.java] Review and potentially remove nextDigest
- [BeefyState.java] Review and potentially remove lastVote
- [BeefyState.java] Review and potentially remove initializeNextDigest
- [BeefyState.java] Generate key ownership proof
- [BeefyState.java] Submit report double voting to Beefy api path
- [EpochState.java] Add methods to load epoch state data
- [EpochState.java] Add methods to store epoch state data
- [BeefyService.java] Handle restarting of main loop

## Runtime TODOs
- [RuntimeVersion.java] Remove need for public @Setter
- [RuntimeVersion.java] Fix naming inconsistency with ApiVersions
- [ApiVersions.java] Review returning -1 as default value
- [TrieHostFunctions.java] Figure out how state version affects proof verification
- [OffchainHostFunctions.java] Add to transaction pool when implemented
- [AsyncExecutor.java] Create centralized retry function
- [HostConfig.java] Complete implementation

## Build TODOs
- [build.gradle.kts] Publish imported packages to maven repository and import them