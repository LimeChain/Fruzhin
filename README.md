![Fruzhin-Cover-Black](https://github.com/LimeChain/Fruzhin/assets/29047760/8e617c9a-005d-44b7-b2bc-d14cc6860726)

Fruzhin is a Java Implementation of the Polkadot Host. The ultimate goal for Fruzhin is to be able to function as an
authoring and relaying node, increasing security of the Polkadot Protocol. It's been funded by
[Polkadot Pioneers Prize](https://polkadot.polkassembly.io/child_bounty/238).
> **Warning**
> Fruzhin is in pre-production state

# Status

- [x] Light Client
- [ ] Full Node
- [ ] Authoring Node
- [ ] Relaying Node

# Getting started

## Clone

```bash
git clone https://github.com/LimeChain/Fruzhin.git
cd Fruzhin
```

## Build

```bash
./gradlew build
```

## Running Fruzhin

### Sync with official chain

```bash
java -jar build/libs/fruzhin-0.1.0.jar -n {network}
```

- `network` can be `westend`, `polkadot` or `kusama`

### Local development

1. Create a local chain specification file by making a copy of `genesis/westend-local-example.json` in the same folder.
2. Rename the copied file to `westend-local.json`
3. Run a local Polkadot node with ```polkadot --dev``` command.
4. Fetch the chain spec

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "sync_state_genSyncSpec", "params": [true]}' http://localhost:9944
   ```

   The `lightSyncState` field is important for the light client to
   work. Without
   it, the light client won't have a checkpoint to start from
   and could be long-range attacked


5. Get the local boot nodes.

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "system_localListenAddresses"}' http://localhost:9944
   ```

   Paste the response into the `bootNodes` field of the chain spec.


6. Get the genesis hash

   ```bash
   curl -H 'Content-Type: application/json' -d '{"id": 1, "jsonrpc": "2.0","method": "chain_getBlockHash","params": [0]}' http://localhost:9944/
   ```

   Paste the response into the `LOCAL` field of `GenesisHash`.


7. Build Host
   ```
   ./gradlew build
   ```
8. Run Host
   ```
   java -jar  -Djava.library.path=./artifacts/darwin-amd64 build/libs/fruzhin-0.1.0.jar -n local
   ```
