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