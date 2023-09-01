package com.limechain.runtime.hostapi.functions;

import com.limechain.runtime.hostapi.HostApi;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.tomcat.util.buf.HexUtils;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

@UtilityClass
@Log
public class MiscellaneousHostFunctions extends HostApi {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostFunctions.getImportObject("ext_misc_print_num_version_1", argv -> {

                }, List.of(Type.I64)),
                HostFunctions.getImportObject("ext_misc_print_utf8_version_1", argv -> {

                }, List.of(Type.I64)),
                HostFunctions.getImportObject("ext_misc_print_hex_version_1", argv -> {
                    extMiscPrintHex((long) argv.get(0));
                    }, List.of(Type.I64)),
                HostFunctions.getImportObject("ext_misc_runtime_version_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I64),
                HostFunctions.getImportObject("ext_logging_log_version_1", argv -> {
                    extLoggingLog((Integer) argv.get(0), (Long) argv.get(1), (Long) argv.get(2));
                }, Arrays.asList(Type.I32, Type.I64, Type.I64)),
                HostFunctions.getImportObject("ext_logging_max_level", argv -> {
                    return argv;
                }, List.of(), Type.I64),
                HostFunctions.getImportObject("ext_panic_handler_abort_on_panic_version_1", argv -> {
                    extPanicHandlerAbortOnPanicVersion1((Long) argv.get(0));
                }, List.of(Type.I64)));
    }

    private static void extMiscPrintHex(long pointer) {
        byte[] data = getDataFromMemory(pointer);
        log.info(HexUtils.toHexString(data));
    }

    private static void extLoggingLog(int level, long targetPtr, long messagePtr) {
        byte[] target = getDataFromMemory(targetPtr);
        byte[] message = getDataFromMemory(messagePtr);

        log.log(getLogLevel(level), new String(target) + ": " + new String(message));
    }


    private static void extPanicHandlerAbortOnPanicVersion1(long messagePtr) {
        byte[] data = getDataFromMemory(messagePtr);
        log.severe(String.valueOf(data));
    }

    private static Level getLogLevel(int i) {
        return switch (i) {
            case 0 -> Level.SEVERE;
            case 1 -> Level.WARNING;
            case 2 -> Level.INFO;
            case 3 -> Level.FINE;
            default -> Level.FINEST;
        };
    }
}
