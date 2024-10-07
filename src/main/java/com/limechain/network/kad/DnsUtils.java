package com.limechain.network.kad;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DnsUtils {
    /**
     * Makes a dns lookup and changes the address to an equal ip4 address
     *
     * @param bootNode
     * @return bootNode in ip4 format
     */
    public static String dnsNodeToIp4(final String bootNode) {
        int prefixEnd = bootNode.indexOf('/', 1) + 1;
        String prefix = bootNode.substring(0, prefixEnd);

        String newBootNode = bootNode;
        if (prefix.equals("/dns/")) {
            int domainEnd = bootNode.indexOf('/', prefixEnd);
            String domain = bootNode.substring(prefixEnd, domainEnd);
            String postfix = bootNode.substring(domainEnd);

            try {
                InetAddress address = InetAddress.getByName(domain);
                newBootNode = "/ip4/" + address.getHostAddress() + postfix;
            } catch (UnknownHostException e) {
                log.log(Level.WARNING, "Unknown domain for bootstrap node address: " + domain);
                log.log(Level.FINE, "Domain exception: ", e);
            }
        }
        return newBootNode;
    }

}
