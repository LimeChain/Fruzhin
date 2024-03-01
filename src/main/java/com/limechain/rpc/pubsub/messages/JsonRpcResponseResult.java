package com.limechain.rpc.pubsub.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class JsonRpcResponseResult {
    private String result;
    //TODO: Add subscription information.
}