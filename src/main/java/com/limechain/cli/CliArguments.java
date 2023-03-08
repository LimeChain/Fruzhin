package com.limechain.cli;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CliArguments {
    private final String network;
    private final String dbPath;
}
