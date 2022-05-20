package com.sequoiacm.infrastructure.lock.curator;

import com.sequoiacm.infrastructure.common.ZkAcl;
import com.sequoiacm.infrastructure.common.ZkAclUtils;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

import java.util.Collections;
import java.util.List;

public class CuratorLockTools {
    private static String rootPath = "/scm/lock";
    private static CuratorZKCleaner zkCleaner = CuratorZKCleaner.getInstance();

    public static CuratorFramework createClient(String connectString, boolean enableContainer,
            ZkAcl acl) throws Exception {
        CuratorFramework client = null;
        try {
            Builder clientBuiler = CuratorFrameworkFactory.builder();
            if (!enableContainer) {
                clientBuiler.dontUseContainerParents();
            }
            clientBuiler.connectString(connectString)
                    .retryPolicy(new ExponentialBackoffRetry(CuratorLockProperty.BASESLEEPTIMEMS,
                            CuratorLockProperty.MAXRETRIES))
                    .sessionTimeoutMs(CuratorLockProperty.SESSIONTIMEOUTMS)
                    .connectionTimeoutMs(CuratorLockProperty.CONNECTIONTIMEOUTMS);

            if (acl.isEnabled()) {
                acl.validate();
                final List<ACL> aclList = Collections
                        .singletonList(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.AUTH_IDS));
                String idStr = ScmFilePasswordParser.parserFile(acl.getId()).getPassword();
                if (!acl.isIdAvailable(idStr)) {
                    throw new IllegalArgumentException("id file is invalid:" + acl.getId());
                }
                clientBuiler.authorization(ZkAclUtils.getDefaultScheme(), idStr.getBytes());
                clientBuiler.aclProvider(new ACLProvider() {

                    @Override
                    public List<ACL> getDefaultAcl() {
                        return aclList;
                    }

                    @Override
                    public List<ACL> getAclForPath(String path) {
                        return aclList;
                    }
                });
            }
            client = clientBuiler.build();
            client.start();
            if (acl.isEnabled()) {
                grantBasicPathAcl(client);
            }
        }
        catch (Exception e) {
            throw e;
        }
        return client;
    }

    private static void grantBasicPathAcl(CuratorFramework client) throws Exception {
        List<ACL> aclList = Collections.singletonList(
                new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.AUTH_IDS));
        List<String> list = ZkAclUtils.getBasicZkPathList();
        for (String path : list) {
            if (client.checkExists().forPath(path) != null) {
                client.setACL().withACL(aclList).forPath(path);
            }
        }
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
