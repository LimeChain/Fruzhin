# Fruzhin's Zombienet tests

## How to run the tests

_Tests have to be run from the root of the repository._

1. Run `gradle build` and run it after every change in the source code before executing the tests.
2. You can run the first test with the following command (for others just substitute the correct test name):

```shell
nix run github:paritytech/zombienet -- test -p native ./zombienet/0001-light-client-header-verification.zndsl
```