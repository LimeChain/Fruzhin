package com.limechain.lightclient;

// TODO: Think of a better name... that's tautological, simply `Client` is awfully generic
public interface NodeClient {
    /**
     * Starts the client by assigning all dependencies and services from the spring boot application's context
     *
     * @apiNote the RpcApp is assumed to have been started
     *          before constructing the clients in our current implementations,
     *          as starting the clients relies on the application context
     */
    void start();
}
