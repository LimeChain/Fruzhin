package com.limechain.exception;

public class PrometheusServerStartException extends RuntimeException {
    public PrometheusServerStartException(Throwable e) {
        super(e);
    }
}
