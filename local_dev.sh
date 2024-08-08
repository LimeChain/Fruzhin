#!/bin/bash

# Fetch chain spec
chain_spec_resp=$(curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "sync_state_genSyncSpec", "params": [true]}' http://localhost:9944)
# Fetch boot nodes
boot_nodes_resp=$(curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "system_localListenAddresses"}' http://localhost:9944)

# Remove "raw" to comply with Fruzhin project spec
output_json=$(echo "$chain_spec_resp" | jq '.result | .genesis |= . + .raw | del(.genesis.raw)')
# Extract boot nodes result from RPC json
boot_nodes=$(echo "$boot_nodes_resp" | jq '.result')
# Paste boot nodes
final_json=$(echo "$output_json" | jq --argjson bootNodes "$boot_nodes" '.bootNodes = $bootNodes')

echo "$final_json" > "./src/main/webapp/genesis/westend-local.json"