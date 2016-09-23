package org.apache.problem;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.list;

/**
 * Created by manishk on 9/22/16.
 */
public class HostUtil {

    public static List<String> getNetworkInterfaces() throws SocketException {
        return list(NetworkInterface.getNetworkInterfaces()).stream()
                .flatMap(ni -> list(ni.getInetAddresses()).stream())
                .filter(address -> !address.isAnyLocalAddress())
                .filter(address -> !address.isMulticastAddress())
                .filter(address -> !address.isLoopbackAddress())
                .filter(address -> !(address instanceof Inet6Address))
                .map(InetAddress::getHostAddress)
                .collect(Collectors.toList());
    }
}
