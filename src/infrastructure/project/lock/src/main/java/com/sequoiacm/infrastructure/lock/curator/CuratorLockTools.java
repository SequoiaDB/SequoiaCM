package com.sequoiacm.infrastructure.lock.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class CuratorLockTools {
    private static String rootPath = "/scm/lock";
    private static CuratorZKCleaner zkCleaner = CuratorZKCleaner.getInstance();

    public static CuratorFramework createClient(String connectString, boolean enableContainer)
            throws Exception {
        CuratorFramework client = null;
        try {
            Builder clientBuiler = CuratorFrameworkFactory.builder();
            if (!enableContainer) {
                clientBuiler.dontUseContainerParents();
            }
            client = clientBuiler.connectString(connectString)
                    .retryPolicy(new ExponentialBackoffRetry(CuratorLockProperty.BASESLEEPTIMEMS,
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
        int lastIndex = getLastIndex(path);
        if (lastIndex == -1) {
            return rootPath;
        }
        String parentPath = getAndRecordParentPath(path, lastIndex);
        return parentPath + CuratorLockProperty.LOCK_PATH_SEPERATOR + path[lastIndex];

    }

    private static String getAndRecordParentPath(String[] path, int lastIndex) {
        StringBuilder parentPath = new StringBuilder(rootPath);
        for (int i = 0; i < lastIndex; i++) {
            if (path[i] != null && !"".equals(path[i])) {
                parentPath.append(CuratorLockProperty.LOCK_PATH_SEPERATOR).append(path[i]);
            }
        }
        zkCleaner.putPath(parentPath.toString());
        return parentPath.toString();
    }

    private static int getLastIndex(String[] path) {
        int lastIndex = path.length - 1;
        while (lastIndex >= 0) {
            String lastName = path[lastIndex];
            if (lastName != null && !"".equals(lastName)) {
                break;
            }
            lastIndex--;
        }
        return lastIndex;
    }
}
