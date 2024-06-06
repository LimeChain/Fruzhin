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

Fruzhin only works with Java 21. 

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
In order to use the Fruzhin node for local development you will first need to start another node that would serve as a
peer. 

For the sake of this example we will use [Paritytech's implementation](https://github.com/paritytech/polkadot-sdk).
If you are not familiar with how to run a node see [this](https://wiki.polkadot.network/docs/maintain-sync#setup-instructions).

Once you have successfully built the Polkadot project run the node via ``polkadot --dev``.
(The node starts on port 9944 by default)

Now you have 2 options:
- Use the automated `local_dev.sh` script
- Manual setup.

#### Automated script
1. Install [JQ](https://github.com/jqlang/jq).

   `sudo apt-get install jq` Ubuntu
   
   `brew install jq` MacOS

2. Head to the main directory of Fruzhin execute the script `./local_dev.sh`.

#### Manual setup
1. Fetch the chain spec

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "sync_state_genSyncSpec", "params": [true]}' http://localhost:9944
   ```

   The `lightSyncState` field is important for the light client to
   work. Without it, the light client won't have a checkpoint to start from
   and could be long-range attacked

2. Create a new `westend-local.json` inside of the `genesis` project directory.
3. Copy the contents of the `result` field from the fetched chain spec into the newly created `westend-local.json`.
4. In order to comply with the project requirements change the json structured as follows:

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

5. Fetch the local boot nodes.

   ```bash
   curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "system_localListenAddresses"}' http://localhost:9944
   ```

   Paste the response into the `bootNodes` field of the `westend-local.json` chain spec.

#### Build & Run
1. Build project
   ```
   ./gradlew build
   ```
2. Run Fruzhin
   ```
   java -jar build/libs/Fruzhin-0.1.0.jar -n 'local' --node-mode 'full'/'light' --sync-mode 'full'/'warp' --db-recreate true/false
   ```