package com.sequoiacm.infrastructure.lock.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class CuratorLockTools {
    private static String separator = "/";
    private static String rootPath = "/scm/lock";

    public static CuratorFramework createClient(String connectString) throws Exception {
        CuratorFramework client = null;
        try {
            client = CuratorFrameworkFactory
                    .builder()
                    .connectString(connectString)
                    .retryPolicy(
                            new ExponentialBackoffRetry(CuratorLockProperty.BASESLEEPTIMEMS,
                                    CuratorLockProperty.MAXRETRIES))
                                    .sessionTimeoutMs(CuratorLockProperty.SESSIONTIMEOUTMS)
                                    .connectionTimeoutMs(CuratorLockProperty.CONNECTIONTIMEOUTMS).build();
            client.start();
        }
        catch (Exception e) {
            throw e;
        }
        return client;
    }

    public static String getRootPath() {
        return rootPath;
    }

    public static String getLockPath(String[] path) {
        String lockPath = "";
        for (int i = 0; i < path.length; i++) {
            if (path[i] != null && !"".equals(path[i])) {
                lockPath += separator + path[i];
            }
        }

        return rootPath + lockPath;
    }

}
