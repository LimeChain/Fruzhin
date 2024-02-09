package com.limechain.runtime;

import com.limechain.rpc.server.AppBean;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class RuntimeBuilderTest {

    @Test
    @Disabled("Very lengthy, needs either refactoring in the code being tested or a lot of effort into mocking")
    void buildsRuntimeWithVersionInCustomSection() throws IOException {
        try (InputStream wasmBytesInput = this.getClass().getResourceAsStream("/runtime_version_custom_section.wasm")) {
            byte[] wasmBytes = wasmBytesInput.readAllBytes();

            try (MockedStatic<AppBean> mockedStatic = mockStatic(AppBean.class)) {
                mockedStatic.when(() -> AppBean.getBean(any())).thenReturn(null);
                new RuntimeBuilder().buildRuntime(wasmBytes); //.... we can't build this runtime
            }
        }

    }

}