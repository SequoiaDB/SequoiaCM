package com.sequoiacm.infrastructure.common;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetUtil {
    public static boolean isSameHost(String host1, String host2) throws UnknownHostException {
        if (host1.equals(host2)) {
            return true;
        }
        String host1Address = InetAddress.getByName(host1).getHostAddress();
        String host2Address = InetAddress.getByName(host2).getHostAddress();
        return host1Address.equals(host2Address);
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
}
