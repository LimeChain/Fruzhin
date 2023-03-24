# Polkadot Java Host

Repository is under active development. Goal for first phase is to have a functioning light client.

# Build & run

1. Run smoldot light client.
    - See (https://github.com/smol-dot/smoldot#wasm-light-node)[here] for information how to do that
2. ```./gradlew build```
3. ```java -jar build/libs/java-host-1.0-SNAPSHOT.jar -n {network}```
    - `network` can be `westend`, `polkadot` or `kusama`

# Development
1. Run smoldot light client as mentioned above
2. Make a copy of genesis/westend-local-example.json. Rename it to westend-local.json and add your boot nodes.
2. ```./gradlew build```
3. ```java -jar build/libs/java-host-1.0-SNAPSHOT.jar -n local```
