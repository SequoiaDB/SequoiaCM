package com.sequoiacm.infrastructure.vote.curator;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import com.sequoiacm.infrastructure.vote.ScmLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmNotLeaderAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ScmCuratorVoteListener implements LeaderLatchListener {
    private static final Logger logger = LoggerFactory.getLogger(ScmCuratorVoteListener.class);
    private ScmLeaderAction leaderAction;
    private ScmNotLeaderAction notLeaderAction;
    private volatile ScmVoteResultType voteResult = ScmVoteResultType.SLAVER;

    public ScmCuratorVoteListener(ScmLeaderAction leaderAction,
            ScmNotLeaderAction notLeaderAction) {
        this.leaderAction = leaderAction;
        this.notLeaderAction = notLeaderAction;
    }

    @Override
    public void isLeader() {
        logger.info("################leader init start#######################");
        leaderAction.run();
        voteResult = ScmVoteResultType.LEADER;
        logger.info("################leader init end#######################");
    }

    @Override
    public void notLeader() {
        voteResult = ScmVoteResultType.SLAVER;
        notLeaderAction.run();

    }

    public ScmVoteResultType getVoteResult() {
        return voteResult;
    }
}
