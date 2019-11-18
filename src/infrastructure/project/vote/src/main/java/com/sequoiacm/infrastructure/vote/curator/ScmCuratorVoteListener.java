package com.sequoiacm.infrastructure.vote.curator;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import com.sequoiacm.infrastructure.vote.ScmLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmNotLeaderAction;


class ScmCuratorVoteListener implements LeaderLatchListener {
    private ScmLeaderAction leaderAction;
    private ScmNotLeaderAction notLeaderAction;
    public ScmCuratorVoteListener(ScmLeaderAction leaderAction,
            ScmNotLeaderAction notLeaderAction) {
        this.leaderAction = leaderAction;
        this.notLeaderAction = notLeaderAction;
    }

    @Override
    public void isLeader() {
        leaderAction.run();
    }

    @Override
    public void notLeader() {
        notLeaderAction.run();
    }
}
