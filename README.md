# Polkadot Java Host

Repository is under active development. Goal for first phase is to have a functioning light client.

# Build & run

1. ```./gradlew build```
2. ```java -jar build/libs/java-host-1.0-SNAPSHOT.jar -n {network}```

- `network` can be `westend`, `polkadot`, `kusama` or `local`

### Local development

1. Create a local chain specification file:
    1. Make a copy of `genesis/westend-local-example.json` in the same folder.
    2. Rename the copied file to `westend-local.json`
    3. Add your local boot nodes and
       other information if necessary.
2. ```./gradlew build```
3. ```java -jar build/libs/java-host-1.0-SNAPSHOT.jar -n local```