# Fruzhin's Zombienet tests

## How to run the tests

_Tests have to be run from the root of the repository._

1. Run `gradle clean build` and run it after every change in the source code before executing the tests.
2. You can run the first test with the following command (for others just substitute the correct test name):

```shell
nix run github:paritytech/zombienet -- test -p native ./zombienet/0001-light-client-header-verification.zndsl
```

Although the `chain_spec_path` is specified in the TOML file, the actual chain spec used by the node is determined by 
the value of `-Dgenesis.path.local` argument in the `start-node.sh`. Additionally, the `inject-env-and-start.sh` script
injects the BABE, GRANDPA, and BEEFY keys for the Alice as environment variables before starting the node.