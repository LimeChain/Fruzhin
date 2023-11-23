package com.limechain.lightclient;

public interface HostNode {
    /**
     * Starts the client by assigning all dependencies and services from the spring boot application's context
     *
     * @apiNote the RpcApp is assumed to have been started
     *          before constructing the clients in our current implementations,
     *          as starting the clients relies on the application context
     */
    void start();
}
