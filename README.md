![Fruzhin-Cover-Black](https://github.com/LimeChain/Fruzhin/assets/29047760/8e617c9a-005d-44b7-b2bc-d14cc6860726)

Fruzhin is a Java Implementation of the Polkadot Host. The ultimate goal for Fruzhin is to be able to function as an
authoring and relaying node, increasing security of the Polkadot Protocol. It's been funded by
[Polkadot Pioneers Prize](https://polkadot.polkassembly.io/child_bounty/238).
> **Warning**
> Fruzhin is in pre-production state

# Status

- [x] Light Client
- [x] Full Node
- [ ] Authoring Node
- [ ] Relaying Node

# Getting started

## Clone

```bash
git clone https://github.com/LimeChain/Fruzhin.git
cd Fruzhin
```

## Setup & Build steps

### Java Version

Fruzhin only works
with [Java 17 Coretto](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html). Using any other
version may cause "cannot calculate secret" errors when running the node:

```
org.bouncycastle.tls.crypto.TlsCryptoException: cannot calculate secret
```

If you have multiple java version installed please make sure you're using 17:

```
export JAVA_HOME=`/usr/libexec/java_home -v 17.0.8`
```

### Wasmer-Java dylib setup

```
Note: This step will be automated in the future
```

Apple silicon users can skip this step.
For now, you will have to manually grab the compiled `wasmer-java` dynamic library
file from the subfolder under `./wasmer-setup` corresponding to your architecture type.
Copy the file to the Java `Extensions` folder:

```
/Library/Java/Extensions
```

### Build

```bash
./gradlew build
```

## Running Fruzhin

### Sync with official chain

```bash
java --enable-preview -jar build/libs/fruzhin-0.1.0.jar -n polkadot -node-mode full -sync-mode full
```

- `--enable-preview` flag is necessary as we are using java preview features
- `-n`(network) could be `westend`, `polkadot` or `kusama`
- `-node-mode` could be `full` or `light`
- `-sync-mode` could be `full` or `warp`

### Local development

1. Create a local chain specification file by making a copy of `genesis/westend-local-example.json` in the same folder.
2. Rename the copied file to `westend-local.json`
3. Run a local Polkadot node with ```polkadot --dev``` command.
4. Fetch the chain spec

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "sync_state_genSyncSpec", "params": [true]}' http://localhost:9922
   ```

   The `lightSyncState` field is important for the light client to
   work. Without
   it, the light client won't have a checkpoint to start from
   and could be long-range attacked


5. Get the local boot nodes.

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "system_localListenAddresses"}' http://localhost:9922
   ```

   Paste the response into the `bootNodes` field of the chain spec.


6. Build Host
   ```
   ./gradlew build
   ```
7. Run Host
   ```
   java -jar build/libs/fruzhin-0.1.0.jar -n local
   ```