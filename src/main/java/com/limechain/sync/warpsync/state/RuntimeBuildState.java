package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Imports;
import org.wasmer.Instance;
import org.wasmer.Module;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;


/**
 * Performs some runtime calls in order to obtain the current consensus-related parameters
 * of the chain. This might require obtaining some storage items, in which case they will also
 * be downloaded from a source in the Chain Information Download State
 */
@Log
public class RuntimeBuildState implements WarpSyncState {
    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Done with runtime build");
        sync.setState(new ChainInformationDownloadState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        // TODO: After runtime is downloaded, we are downloading and computing the information of the chain
        try {
            Module module = new Module(sync.getRuntime());

            Imports imports = Imports.from(Arrays.asList(
                    new ImportObject.FuncImport("env", "ext_storage_set_version_1", argv -> {
                        System.out.println("Message printed in the body of 'ext_storage_set_version_1'");
                        return argv;
                    }, Arrays.asList(Type.I64, Type.I64), Collections.emptyList()),
                    new ImportObject.FuncImport("env", "ext_storage_get_version_1", argv -> {
                        System.out.println("Message printed in the body of 'ext_storage_get_version_1'");
                        return argv;
                    }, Collections.singletonList(Type.I64), Collections.singletonList(Type.I64)),
                    new ImportObject.MemoryImport("env", 20, false)), module);
            System.out.println("Instantiating module");
            Instance instance = module.instantiate(imports);

            System.out.println("Calling exported function 'Core_initialize_block' as it calls both of the imported functions");
            instance.exports.getFunction("Core_initialize_block").apply(1, 2);

            instance.close();
        } catch (UnsatisfiedLinkError e) {
            log.log(Level.SEVERE, "Error loading wasm module");
            log.log(Level.SEVERE, e.getMessage(), e.getStackTrace());
        }
    }
}
