package com.limechain.sync.warpsync;

import lombok.Getter;
import org.wasmer.Instance;
import org.wasmer.Module;

import static com.limechain.sync.warpsync.RuntimeBuilder.getImports;

@Getter
public class Runtime {
    RuntimeVersion version;
    Instance instance;
    int heapPages;

    public Runtime(Module module, int heapPages, RuntimeVersion version) {
        this.version = version;
        this.heapPages = heapPages;
        this.instance = module.instantiate(getImports(module, heapPages));
    }
}
