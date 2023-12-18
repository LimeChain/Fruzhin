package com.limechain.trie.structure.decoded.node;

import org.jetbrains.annotations.NotNull;

public record StorageValue(@NotNull byte[] value, boolean isHashed) { }
