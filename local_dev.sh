#!/bin/bash

if [ "$2" = "true" ]; then
  if [ $# != 5 ]; then
    echo "Usage: $(basename $0) PORT(INT) START_FRUZHIN(BOOL) NODE_MODE(STRING) SYNC_MODE(STRING) DB_RECREATE(BOOL)"
    exit 1
  fi
elif [ $# != 2 ]; then
    echo "Usage: $(basename $0) PORT(INT) START_FRUZHIN(BOOL)"
    exit 1
fi

# Fetch chain spec
chain_spec_resp=$(curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "sync_state_genSyncSpec", "params": [true]}' http://localhost:$1)
# Fetch boot nodes
boot_nodes_resp=$(curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "system_localListenAddresses"}' http://localhost:$1)

# Remove "raw" to comply with Fruzhin project spec
output_json=$(echo "$chain_spec_resp" | jq '.result | .genesis |= . + .raw | del(.genesis.raw)')
# Extract boot nodes result from RPC json
boot_nodes=$(echo "$boot_nodes_resp" | jq '.result')
# Paste boot nodes
final_json=$(echo "$output_json" | jq --argjson bootNodes "$boot_nodes" '.bootNodes = $bootNodes')

echo "$final_json" > "./genesis/westend-local.json"

if [ "$2" = "true" ]; then
    ./gradlew clean build
     java -jar build/libs/Fruzhin-0.1.0.jar -n local --node-mode $3 --sync-mode $4 --db-recreate $5
fi