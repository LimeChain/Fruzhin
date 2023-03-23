package com.limechain.network;

import com.limechain.config.HostConfig;
import com.limechain.network.kad.KademliaService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.logging.Level;

@Component
@Log
@Getter
@Setter
public class Network {
    private static Network network;
    public static KademliaService kademliaService;
    LinkedHashMap<String, String> peerAddresses;

    private Network(HostConfig hostConfig) {
        kademliaService = new KademliaService("/dot/kad", hostConfig.getBootstrapNodes());
        peerAddresses = new LinkedHashMap<>();
    }

    public static Network getInstance() {
        if (network != null) {
            return network;
        }
        throw new AssertionError("Network not initialized.");
    }

    public static Network initialize(HostConfig hostConfig) {
        if (network != null) {
            log.log(Level.WARNING, "Network module already initialized.");
            return network;
        }
        network = new Network(hostConfig);
        log.log(Level.INFO, "Initialized network module!");
        return network;
    }

    @Scheduled(fixedDelay = 10000)
    public void findPeers() throws InterruptedException {
        log.log(Level.INFO, "Finding peers");
        try{
            kademliaService.findNewPeers(peerAddresses);
        } catch (Exception e){
            log.log(Level.SEVERE, "Error: "+ e.getMessage());
        }
        log.log(Level.INFO, "Peers: " + peerAddresses.size());
        if(peerAddresses.size()>2){
            Thread.sleep(10000);
        }
    }
}
