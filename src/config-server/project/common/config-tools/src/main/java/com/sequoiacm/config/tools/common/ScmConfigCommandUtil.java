package com.sequoiacm.config.tools.common;

import java.util.ArrayList;
import java.util.List;

public class ScmConfigCommandUtil {

    public static List<String> parseListUrls(String gatewayUrl) {
        List<String> urls = new ArrayList<>();
        String[] arr = gatewayUrl.split(",");
        for (String url : arr) {
            urls.add(url);
        }
        return urls;
    }
}
