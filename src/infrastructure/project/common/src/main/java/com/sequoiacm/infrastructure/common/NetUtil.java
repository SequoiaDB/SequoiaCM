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
}
