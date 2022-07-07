package com.sequoiacm.infrastructure.common;

import java.net.URI;

public class CheckRuleUtils {
    public static boolean isConformHostNameRule(String hostName){
        //feign 使用 java.net.URI 来解析服务名,使用URI来判断是否符合主机名规范
        URI asUri = URI.create("http://" + hostName);
        String clientName = asUri.getHost();
        if (hostName == null || !hostName.equals(clientName)) {
           return false;
        }
        return true;
    }
}
