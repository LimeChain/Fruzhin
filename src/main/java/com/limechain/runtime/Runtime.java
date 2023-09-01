package com.limechain.runtime;

import lombok.Getter;
import lombok.extern.java.Log;
import org.wasmer.Instance;
import org.wasmer.Module;

import java.util.logging.Level;

import static com.limechain.runtime.RuntimeBuilder.getImports;

@Getter
@Log
public class Runtime {
    private RuntimeVersion version;
    private Instance instance;
    private int heapPages;

    public Runtime(Module module, int heapPages) {
        this.heapPages = heapPages;
        this.instance = module.instantiate(getImports(module));
    }

    /**
     * Calls a runtime function without arguments.
     * Arguments for runtime functions are to be supported in future version
     *
     * @param functionName name of the function to be called
     * @return response containing encoded memory pointer and data length to imported memory
     */
    public Object[] call(String functionName) {
        log.log(Level.INFO, "Making a runtime call: " + functionName);
        try {
            Object[] response = this.instance.exports.getFunction(functionName).apply(0, 0);
            log.log(Level.INFO, "Runtime call response:" + response);
            return response;
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return null;
        }
    }

    public void setVersion(RuntimeVersion runtimeVersion) {
        this.version = runtimeVersion;
    }
}
