package com.limechain.runtime;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.wasmer.Instance;
import org.wasmer.Memory;
import org.wasmer.Module;

import java.util.List;
import java.util.logging.Level;

import static com.limechain.runtime.RuntimeBuilder.getImports;

@Getter
@Log
public class Runtime {
    @Setter
    private RuntimeVersion version;
    private Instance instance;
    private int heapPages;
    @Getter
    private Memory memory;

    public Runtime(Module module, int heapPages) {
        this.heapPages = heapPages;
        this.instance = module.instantiate(getImports(this, module, heapPages));
        this.memory = this.instance.exports.getMemory("memory");
    }

    /**
     * Calls a runtime function without arguments.
     * Arguments for runtime functions are to be supported in future version
     *
     * @param functionName name of the function to be called
     * @return response containing encoded memory pointer and data length to imported memory
     */
    public byte[] call(String functionName) {
        log.log(Level.INFO, "Making a runtime call: " + functionName);
        try {
            Object[] response = this.instance.exports.getFunction(functionName).apply(0, 0);
            long memPointer = (long) response[0];
            int ptr = (int) memPointer;
            int ptrLength = (int) (memPointer >> 32);
            byte[] data = new byte[ptrLength];

            this.memory.buffer().get(ptr, data, 0, ptrLength);
            log.log(Level.INFO, "Runtime call response:" + data);
            return data;
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return null;
        }
    }

    public void printLogData(List<Number> logPointers){
        for (int i = 0; i < logPointers.size(); i++) {
            long pointer = logPointers.get(i).longValue();
            int ptr = (int) pointer;
            int ptrSize = (int) (pointer >> 32);
            byte[] data = new byte[ptrSize];
            memory.buffer().get(ptr, data, 0, ptrSize);
            System.out.println(new String(data));
        }
    }
}
