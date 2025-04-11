#!/bin/bash

echo "📦 Make sure Docker is up and running!"

echo ""
echo "🚀 Starting Substrate node (Westend local)..."
docker run -d --rm --platform=linux/amd64 \
  -p 9944:9944 -p 30333:30333 \
  --name substrate-node \
  parity/polkadot:latest \
  --dev \
  --chain=westend-local \
  --rpc-port 9944 \
  --rpc-cors=all \
  --unsafe-rpc-external

echo ""
echo "📦 Running clean build to generate the JAR file..."
./gradlew clean build jar -x test

echo ""
echo "🔧 Applying local dev setup (chainspec copy)..."
bash ./local_dev.sh

echo ""
echo "🧟 Running Zombienet test: 0001"
nix run github:paritytech/zombienet -- test -p native ./zombienet/0001-light-client-header-verification.zndsl

echo ""
echo "🧟 Running Zombienet test: 0002"
nix run github:paritytech/zombienet -- test -p native ./zombienet/0002-peer-discovery.zndsl

echo ""
echo "🧟 Running Zombienet test: 0003"
nix run github:paritytech/zombienet -- test -p native ./zombienet/0003-block-execution.zndsl

echo ""
echo "🧹 Shutting down Substrate node..."
docker stop substrate-node

echo ""
echo "✅ All tests complete. Node container stopped."