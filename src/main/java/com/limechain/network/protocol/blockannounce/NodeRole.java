package com.limechain.network.protocol.blockannounce;

import lombok.Getter;

public enum NodeRole {
    FULL(1),
    LIGHT(2),
    AUTHORING(4);

    @Getter
    private final Integer value;

    NodeRole(Integer value) {
        this.value = value;
    }

    /**
     * Matches a string to an enum name by comparing in case-insensitive values
     *
     * @param nodeRole string of
     * @return
     */
    public static NodeRole fromString(String nodeRole) {
        for (NodeRole type : values()) {
            if (type.toString().toLowerCase().equals(nodeRole.toLowerCase())) {
                return type;
            }
        }
        return null;
    }
}
