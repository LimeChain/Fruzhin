# Polkadot Java Host

Repository is under active development. Goal for first phase is to have a functioning light client.

# Build & run

1. ```./gradlew build```
2. ```java -jar build/libs/java-host-1.0-SNAPSHOT.jar -n {network}```

- `network` can be `westend`, `polkadot`, `kusama` or `local`

### Local development

1. Create a local chain specification file by making a copy of `genesis/westend-local-example.json` in the same folder.
2. Rename the copied file to `westend-local.json`
3. Run a local Polkadot node with ```polkadot --dev``` command.
4. Execute

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "sync_state_genSyncSpec", "params": [true]}' http://localhost:9933
   ```

   in order to get the initial chain spec. The `lightSyncState` field is important for the light client to work. Without
   it, the light client won't have a checkpoint to start from
   and could be long-range attacked
5. Execute

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "system_localListenAddresses"' http://localhost:9933
   ```

   in order to get the local boot nodes. Paste the response into the `bootNodes` field of the chain spec.
6. Execute

   ```bash
   curl --request POST \
   --url http://localhost:9933/ \
   --header 'Content-Type: application/json' \
   --data '{"jsonrpc": "2.0","method": "chain_getBlockHash","params": [0],"id": 1}'
   ```

   in order to get the genesis hash. Paste the response into the `LOCAL` field of `GenesisHash`.

7. ```./gradlew build```
8. ```java -jar -Djava.library.path=./artifacts/darwin-amd64 build/libs/java-host-1.0-SNAPSHOT.jar -n local```