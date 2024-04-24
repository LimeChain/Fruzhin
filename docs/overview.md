
---

# Fruzhin Project

## Introduction
![Fruzhin-Cover-Black](https://github.com/LimeChain/Fruzhin/assets/29047760/8e617c9a-005d-44b7-b2bc-d14cc6860726)
Fruzhin is a Java Implementation of the Polkadot Host. The ultimate goal for Fruzhin is to be able to function as an
authoring and relaying node, increasing security of the Polkadot Protocol. It's been funded by
[Polkadot Pioneers Prize](https://polkadot.polkassembly.io/child_bounty/238).

## Getting Started

To get started with Fruzhin, clone the repository and follow the setup instructions:

#### Clone the repository
```bash
git clone https://github.com/LimeChain/Fruzhin.git
cd Fruzhin
```

#### Install java 17 corretto (if you don't already have it)
Setup guide:
- [Windows](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/windows-install.html)
- [Linux](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/generic-linux-install.html)
- [Mac](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/macos-install.html)

#### Build project

```bash
./gradlew build
```

If exception is thrown you will have to manually grab the compiled wasmer-java dynamic library file from the subfolder under ./wasmer-setup corresponding to your architecture type. Copy the file to the Java Extensions folder:

```
/Library/Java/Extensions
```


## Usage

Run the following command to start the blockchain node:

```bash
./java --enable-preview -jar build/libs/Fruzhin-0.1.0.jar -n polkadot --node-mode full --sync-mode full
```

## Contributing

Contributions are welcome! Feel free to contribute by:

- Reporting a bug
- Discussing the current state of the code
- Submitting a fix
- Proposing new features

Please refer to the [Contributing Guide](../CONTRIBUTING.md) for more information.
You can check the file structure in [File Structure](./development/file-structure.md).

## License

Fruzhin is released under the [MIT License](../LICENSE). Please review the license terms for more information.

## Support

For support, you can join our [Discord group](https://discord.gg/nv4NXYUzJV).