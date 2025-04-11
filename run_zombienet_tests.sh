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

tests=(
  "0001-light-client-header-verification.zndsl"
  "0002-peer-discovery.zndsl"
  "0003-block-execution.zndsl"
)

for test in "${tests[@]}"; do
  echo ""
  echo "🧟 Running Zombienet test: $test"
  nix run github:paritytech/zombienet -- test -p native ./zombienet/"$test"
done

echo ""
echo "🧹 Shutting down Substrate node..."
docker stop substrate-node