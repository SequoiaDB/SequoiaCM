package com.sequoiacm.infrastructure.vote.curator;

import java.io.Closeable;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;
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

    public ScmCuratorVote(String zookeeperUrl, String votePath, String id,
            ScmLeaderAction leaderAction, ScmNotLeaderAction notLeaderAction) throws Exception {
        try {
            client = CuratorFrameworkFactory.builder().dontUseContainerParents()
                    .connectString(zookeeperUrl).retryPolicy(new ExponentialBackoffRetry(1000, 3))
                    .build();
            latch = new LeaderLatch(client, votePath, id);
            listener = new ScmCuratorVoteListener(leaderAction, notLeaderAction);
            latch.addListener(listener);
        }
        catch (Exception e) {
            close();
            throw e;
        }
    }

    @Override
    public void startVote() throws Exception {
        client.start();
        latch.start();
    }

    @Override
    public boolean isLeader() {
        return latch.hasLeadership();
    }

    @Override
    public String getLeader() {
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
        closeQuietly(latch);
        latch = null;

        closeQuietly(client);
        client = null;
    }

    @Override
    public String getId() {
        return latch.getId();
    }
}
