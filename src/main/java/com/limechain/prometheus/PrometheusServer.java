package com.limechain.prometheus;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.snapshots.Unit;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.logging.Level;

@Log
public class PrometheusServer {
    private final int port;
    private HTTPServer server;
    private Gauge startTimeGauge;

    public PrometheusServer(int port) {
        this.port = port;
    }

    public HTTPServer start() throws IOException {
        this.registerMetrics();

        this.server = HTTPServer.builder()
                .port(this.port)
                .buildAndStart();

        log.log(Level.INFO, "Prometheus listening on port: " + this.port);

        return this.server;
    }

    public void stop() {
        this.server.stop();
    }

    public void emitStartTime() {
        this.startTimeGauge.set(System.currentTimeMillis() / 1000.0);
    }

    private void registerMetrics() {
        JvmMetrics.builder().register();

        this.startTimeGauge = Gauge.builder()
                // NOTE: This is prefixed with "substrate_" because Zombienet
                // searches for this metric to determine if the node is running
                // and tests won't start if it's not there.
                .name("substrate_process_start_time_seconds")
                .help("Number of seconds between the UNIX epoch and the moment the process started.")
                .unit(Unit.SECONDS)
                .register();
    }
}
