package com.sequoiacm.infrastructure.vote.curator;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;

import com.sequoiacm.infrastructure.common.ZkAcl;
import com.sequoiacm.infrastructure.common.ZkAclUtils;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.vote.ScmLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmNotLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmVote;

public class ScmCuratorVote implements ScmVote {
    private static final Logger logger = LoggerFactory.getLogger(ScmCuratorVote.class);

    private LeaderLatch latch;
    private ScmCuratorVoteListener listener;
    private CuratorFramework client;
    private ZkAcl zkACL;

    public ScmCuratorVote(String zookeeperUrl, String votePath, String id,
            ScmLeaderAction leaderAction, ScmNotLeaderAction notLeaderAction) throws Exception {
        this(zookeeperUrl, new ZkAcl(), votePath, id, leaderAction, notLeaderAction);
    }

    public ScmCuratorVote(String zookeeperUrl, ZkAcl acl, String votePath, String id,
            ScmLeaderAction leaderAction, ScmNotLeaderAction notLeaderAction) throws Exception {
        try {
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .dontUseContainerParents().connectString(zookeeperUrl)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 3));
            if (acl.isEnabled()) {
                acl.validate();
                final List<ACL> aclList = Collections
                        .singletonList(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.AUTH_IDS));
                String idStr = ScmFilePasswordParser.parserFile(acl.getId()).getPassword();
                if (!acl.isIdAvailable(idStr)) {
                    throw new IllegalArgumentException("id file is invalid:" + acl.getId());
                }
                builder.authorization(ZkAclUtils.getDefaultScheme(), idStr.getBytes());
                builder.aclProvider(new ACLProvider() {

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
            zkACL = acl;
            client = builder.build();
            latch = new LeaderLatch(client, votePath, id);
            listener = new ScmCuratorVoteListener(leaderAction, notLeaderAction);
            latch.addListener(listener);
        }
        catch (Exception e) {
            close();
            throw e;
        }
    }

    private static void grantBasicPathAcl(CuratorFramework client) throws Exception {
        List<ACL> aclList = Collections
                .singletonList(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.AUTH_IDS));
        List<String> list = ZkAclUtils.getBasicZkPathList();
        for (String path : list) {
            if (client.checkExists().forPath(path) != null) {
                client.setACL().withACL(aclList).forPath(path);
            }
        }
    }

    @Override
    public void startVote() throws Exception {
        client.start();
        if (zkACL.isEnabled()) {
            grantBasicPathAcl(client);
        }
        latch.start();
    }

    @Override
    public boolean isLeader() {
        return listener.getVoteResult() == ScmVoteResultType.LEADER && latch.hasLeadership();
    }

    @Override
    public String getLeader() {
        if (null == client){
            return "";
        }
        if (!client.getZookeeperClient().isConnected()) {
            return "";
        }

        String leaderId = "";
        try {
            leaderId = latch.getLeader().getId();
        }
        catch (Exception e) {
            logger.warn("getLeader failed", e);
        }

        if (null == leaderId) {
            leaderId = "";
        }

        return leaderId;
    }

    private void closeQuietly(Closeable closeable) {
        if (null == closeable) {
            return;
        }

        try {
            closeable.close();
        }
        catch (Exception e) {
            logger.warn("close closeable instance failed:instance=" + closeable, e);
        }
    }

    @Override
    public void close() {
        listener.notLeader();
        closeQuietly(latch);
        latch = null;

        closeQuietly(client);
        client = null;
    }

    @Override
    public String getId() {
        if (null == latch) {
            return "";
        }
        return latch.getId();
    }
}
