package com.sequoiacm.infrastructure.common;

import java.util.Arrays;
import java.util.List;

public class ZkAclUtils {

    private static final String DEFAULT_SCHEME = "digest";

    private static final List<String> BASIC_ZK_PATH_LIST = Arrays.asList(
            "/",
            "/zookeeper",
            "/zookeeper/quota",
            "/scm");

    public static List<String> getBasicZkPathList() {
        return BASIC_ZK_PATH_LIST;
    }

    public static String getDefaultScheme() {
        return DEFAULT_SCHEME;
    }
}
