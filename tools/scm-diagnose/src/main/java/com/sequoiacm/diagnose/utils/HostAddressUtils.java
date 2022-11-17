package com.sequoiacm.diagnose.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostAddressUtils {
    public static String getLocalHostAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public static String getLocalHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    public static String getIpByHostName(String hostName) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(hostName);
        return inetAddress.getHostAddress();
    }
}
