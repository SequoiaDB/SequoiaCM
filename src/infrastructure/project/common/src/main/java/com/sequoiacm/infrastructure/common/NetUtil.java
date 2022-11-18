package com.sequoiacm.infrastructure.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetUtil {

    private static final Logger logger = LoggerFactory.getLogger(NetUtil.class);

    public static final String LOCALHOST = "127.0.0.1";
    public static final String ANYHOST = "0.0.0.0";

    public static boolean isSameHost(String host1, String host2) throws UnknownHostException {
        if (host1.equals(host2)) {
            return true;
        }
        InetAddress[] host1InetAddress = InetAddress.getAllByName(host1);
        InetAddress[] host2InetAddress = InetAddress.getAllByName(host2);
        for (InetAddress address1 : host1InetAddress) {
            for (InetAddress address2 : host2InetAddress) {
                String host1Address = address1.getHostAddress();
                String host2Address = address2.getHostAddress();
                if (host1Address.equals(host2Address)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getHostAndPort(String url) {
        url = url.replace("http://", "");
        if (url.contains("/")) {
            url = url.substring(0, url.indexOf("/"));
        }
        String[] split = url.split(":");
        String host = split[0];
        String port = split[1];
        if (isIpStr(host)) {
            String hostName = getHostNameByIp(host);
            if (hostName != null) {
                host = hostName;
            }
        }
        return host + ":" + port;
    }

    public static String getHostNameByIp(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            String hostName = inetAddress.getHostName();
            if (!hostName.equals("0.0.0.0")) {
                return hostName;
            }
        }
        catch (UnknownHostException e) {
            return null;
        }
        return null;
    }

    public static boolean isIpStr(String str) {
        try {
            if (str.length() < 7 || str.length() > 15) {
                return false;
            }
            String[] arr = str.split("\\.");
            if (arr.length != 4) {
                return false;
            }
            for (int i = 0; i < 4; i++) {
                int temp = Integer.parseInt(arr[i]);
                if (temp < 0 || temp > 255) {
                    return false;
                }
            }
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static List<String> getAllNetworkInterfaceIp() {
        List<String> ipList = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return ipList;
            }
            while (interfaces.hasMoreElements()) {
                try {
                    NetworkInterface network = interfaces.nextElement();
                    if (!network.isUp()) {
                        continue;
                    }
                    Enumeration<InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        try {
                            InetAddress address = addresses.nextElement();
                            if (isValidAddress(address)) {
                                ipList.add(address.getHostAddress());
                            }
                        }
                        catch (Throwable e) {
                            logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                        }
                    }
                }
                catch (Throwable e) {
                    logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                }
            }
        }
        catch (Throwable e) {
            logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
        }
        return ipList;
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress())
            return false;
        String ipStr = address.getHostAddress();
        return isValidIp(ipStr);
    }


    public static boolean isValidIp(String ipStr) {
        return (ipStr != null && !ANYHOST.equals(ipStr) && !LOCALHOST.equals(ipStr)
                && isIpStr(ipStr));
    }

}
