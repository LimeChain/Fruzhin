package com.limechain.network.kad;

import lombok.extern.java.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

@Log
public class DnsUtils {
    /**
     * Makes a dns lookup and changes the address to an equal ip4 address
     *
     * @param bootNode
     * @return bootNode in ip4 format
     */
    public static String dnsNodeToIp4(String bootNode) {
        int prefixEnd = bootNode.indexOf('/', 1) + 1;
        String prefix = bootNode.substring(0, prefixEnd);

        if (prefix.equals("/dns/")) {
            int domainEnd = bootNode.indexOf('/', prefixEnd);
            String domain = bootNode.substring(prefixEnd, domainEnd);
            String postfix = bootNode.substring(domainEnd);

            try {
                InetAddress address = InetAddress.getByName(domain);
                bootNode = "/ip4/" + address.getHostAddress() + postfix;
            } catch (UnknownHostException e) {
                log.log(Level.WARNING, "Unknown domain for bootstrap node address", e);
            }
        }
        return bootNode;
    }

}
