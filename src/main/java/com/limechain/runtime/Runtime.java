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

    public Runtime(Module module, int heapPages, RuntimeVersion version) {
        this.version = version;
        this.heapPages = heapPages;
        this.instance = module.instantiate(getImports(module, heapPages));
    }

    public Object callNoParams(String functionName) {
        log.log(Level.INFO, "Making a runtime call: " + functionName);
        return instance.exports.getFunction(functionName).apply();
    }

    public Object call(String functionName) {
        log.log(Level.INFO, "Making a runtime call: " + functionName);
        Object response = null;
        //TODO Call adequate params
        try {
            response = instance.exports.getFunction(functionName).apply(0, 0);
            log.log(Level.INFO, "Runtime call response:" + response);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
        }
        return response;
    }
}
