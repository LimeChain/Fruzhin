package com.limechain.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SystemInfoTest {

    @Test
    public void SystemInfo_SetsRole_NoCliOption(){
        SystemInfo systemInfo = new SystemInfo();
        String expectedRole = "LightClient";
        assertEquals(expectedRole, systemInfo.getRole());
    }
}
