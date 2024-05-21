package com.limechain.runtime.research.hybrid.hostapi;

import com.limechain.runtime.research.hybrid.context.Context;
import org.wasmer.ImportObject;

import java.util.List;

public abstract class HostApiImpl {
    protected Context context;
    protected SharedMemory sharedMemory;

    protected HostApiImpl(Context context) {
        this.context = context;
        this.sharedMemory = context.getSharedMemory();
    }

    abstract List<ImportObject> getFunctionImports();
}
