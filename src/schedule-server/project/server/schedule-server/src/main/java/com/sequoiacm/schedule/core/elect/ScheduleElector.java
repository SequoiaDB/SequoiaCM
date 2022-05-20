package com.sequoiacm.schedule.core.elect;

import com.sequoiacm.infrastructure.common.ZkAcl;
import com.sequoiacm.infrastructure.vote.ScmLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmNotLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmVote;
import com.sequoiacm.infrastructure.vote.curator.ScmCuratorVote;

public class ScheduleElector {
    private static ScheduleElector instance = new ScheduleElector();
    private ScmVote vote;

    private String zookeeperUrl;
    private String electPath;
    private String location;
    private ScmLeaderAction leaderAction;
    private ScmNotLeaderAction notLeaderAction;
    private ZkAcl acl;

    private ScheduleElector() {
    }

    public static ScheduleElector getInstance() {
        return instance;
    }

    public void init(String zookeeperUrl, ZkAcl acl, String electPath, String location,
            long revoteInitialInterval, long revoteMaxInterval, double revoteIntervalMultiplier)
            throws Exception {

        this.zookeeperUrl = zookeeperUrl;
        this.electPath = electPath;
        this.location = location;
        this.acl = acl;
        leaderAction = new ScheduleLeaderAction(this, revoteInitialInterval, revoteMaxInterval,
                revoteIntervalMultiplier);
        notLeaderAction = new ScheduleNotLeaderAction(this);

        vote = new ScmCuratorVote(zookeeperUrl, acl, electPath, location, leaderAction,
                notLeaderAction);
        try {
            vote.startVote();
        }
        catch (Exception e) {
            vote.close();
            throw e;
        }
    }

    public boolean isLeader() {
        return vote.isLeader();
    }

    public synchronized void quitAndReVote(long delayToReconnectInMills) throws Exception {
        vote.close();

        if (delayToReconnectInMills > 0) {
            try {
                Thread.sleep(delayToReconnectInMills);
            }
            catch (Exception e) {
                // ignore
            }
        }

        try {
            vote = new ScmCuratorVote(zookeeperUrl, acl, electPath, location, leaderAction,
                    notLeaderAction);
            vote.startVote();
        }
        catch (Exception e) {
            vote.close();
            throw e;
        }
    }

    public void quitAndReVote() throws Exception {
        quitAndReVote(0);
    }

    public String getLeader() {
        return vote.getLeader();
    }

    public String getId() {
        return vote.getId();
    }
}
