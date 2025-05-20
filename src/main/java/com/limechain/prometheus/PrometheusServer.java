package com.limechain.prometheus;

import com.limechain.config.HostConfig;
import com.limechain.exception.misc.PrometheusServerStartException;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.snapshots.Unit;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;

@Log
@Component
public class PrometheusServer {

    private final int port;

    private HTTPServer server;
    private Gauge startTimeGauge;
    private Gauge bestBlockGauge;
    private Gauge finalizedBlockGauge;

    public PrometheusServer(HostConfig hostConfig) {
        this.port = hostConfig.getPrometheusPort();
    }

    public void start() {
        this.registerMetrics();

        try {
            this.server = HTTPServer.builder()
                    .port(this.port)
                    .buildAndStart();

        } catch (IOException e) {
            throw new PrometheusServerStartException(e);
        }

        this.emitStartTime();
        log.info(String.format("Prometheus listening on port: %d", this.port));
    }

    public void stop() {
        this.server.stop();
    }

    private void emitStartTime() {
        this.startTimeGauge.set(System.currentTimeMillis() / 1000.0);
    }

    public void emitBestBlock(BigInteger bestBlockNumber) {
        long longValue = bestBlockNumber.longValueExact();
        this.bestBlockGauge.set(longValue);
    }

    public void emitFinalizedBlock(BigInteger finalizedBlockNumber) {
        long longValue = finalizedBlockNumber.longValueExact();
        this.finalizedBlockGauge.set(longValue);
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

        this.bestBlockGauge = Gauge.builder()
                .name("substrate_best_block_number")
                .help("Best block number of the chain")
                .register();

        this.finalizedBlockGauge = Gauge.builder()
                .name("substrate_last_finalized_block_number")
                .help("Last finalized block number if the chain")
                .register();
    }
}
