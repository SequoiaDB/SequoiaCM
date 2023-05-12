package com.sequoiacm.infrastructure.common;

import java.net.URI;

public class CheckRuleUtils {
    public static boolean isConformSiteNameRule(String siteName) {
        // 站点名需符合主机名要求
        boolean checkRes = checkConformHostNameRule(siteName);
        if (checkRes) {
            if (siteName.contains("\\") || siteName.contains(".")) {
                return false;
            }
        }
        return checkRes;
    }

    public static boolean checkConformHostNameRule(String serviceName) {
        //feign 使用 java.net.URI 来解析服务名,使用URI来判断是否符合主机名规范
        URI asUri = URI.create("http://" + serviceName);
        String clientName = asUri.getHost();
        if (serviceName == null || !serviceName.equals(clientName)) {
            return false;
        }
        return true;
    }
}
