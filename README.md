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
with [Java 21 Coretto](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html). Using any other
version may cause "cannot calculate secret" errors when running the node:

```
org.bouncycastle.tls.crypto.TlsCryptoException: cannot calculate secret
```

If you have multiple java version installed please make sure you're using 21:

```
export JAVA_HOME=`/usr/libexec/java_home -v 21`
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
java -jar build/libs/Fruzhin-0.1.0.jar -n polkadot --node-mode full --sync-mode full
```

- `-n`(network) could be `westend`, `polkadot` or `kusama`
- `--node-mode` could be `full` or `light`
- `--sync-mode` could be `full` or `warp`

### Local development

1. Run a local Polkadot node with ```polkadot --dev``` command. (The default starting port is 9944)
2. Fetch the chain spec

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "sync_state_genSyncSpec", "params": [true]}' http://localhost:9944
   ```

   The `lightSyncState` field is important for the light client to
   work. Without
   it, the light client won't have a checkpoint to start from
   and could be long-range attacked

3. Create a new `westend-local.json` inside of the `genesis` project directory.
4. Copy the contents of the `result` field from the fetched chain spec into the newly created `westend-local.json`.
5. In order to comply with the project requirements change the json structured as follows:

Fetched chain spec
```JSON
{
  "genesis": {
    "raw": {
      "top": {},
      "childrenDefault": {}
    }
  }
}
```

Desired chain spec
```JSON
{
  "genesis": {
     "top": {},
     "childrenDefault": {}
  }
}
```

6. Fetch the local boot nodes.

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "system_localListenAddresses"}' http://localhost:9944
   ```

   Paste the response into the `bootNodes` field of the `westend-local.json` chain spec.

7. Build Host
   ```
   ./gradlew build
   ```
8. Run Host
   ```
   java -jar build/libs/fruzhin-0.1.0.jar -n local
   ```