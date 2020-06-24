package com.radishframework.radish.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;


/**
 * 网络相关工具方法，用于获取服务器端IP地址和端口
 */
public class NetUtils {

    private static final String LOCALHOST = "127.0.0.1";
    private static final String ANYHOST = "0.0.0.0";
    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);
    private static final int MAX_PORT = 65535;
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static volatile InetAddress LOCAL_ADDRESS = null;

    private NetUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static int getAvailablePort(InetAddress hostAddress, int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("port must big than 0");
        }

        for (int i = port; i < MAX_PORT; i++) {
            try (ServerSocket ss = new ServerSocket(i, 50, hostAddress)) {
                return i;
            } catch (IOException e) {
                // continue
            }
        }

        return port;
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return (name != null
                && !ANYHOST.equals(name)
                && !LOCALHOST.equals(name)
                && IP_PATTERN.matcher(name).matches());
    }

    /**
     * 遍历本地网卡，返回第一个合理的IP。
     *
     * @return 本地网卡IP
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Exception e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }

        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
            return localAddress;
        }

        if (interfaces == null) {
            return localAddress;
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface network = interfaces.nextElement();
            Enumeration<InetAddress> addresses = network.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (isValidAddress(address)) {
                    return address;
                }
            }
        }

        logger.error("Could not get local host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }

}