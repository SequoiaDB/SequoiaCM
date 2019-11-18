package com.sequoiacm.infrastructure.test;

import com.sequoiacm.infrastructure.vote.ScmLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmNotLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmVote;
import com.sequoiacm.infrastructure.vote.curator.ScmCuratorVote;

class RetryLeader implements ScmLeaderAction {

    @Override
    public void run() {
        System.out.println("leader");
    }
}

class RetryNotLeader implements ScmNotLeaderAction {

    @Override
    public void run() {
        System.out.println("not leader");
    }
}

public class TestReconnect {
    // @Test
    public void testRetry() throws Exception {
        String id = "1";
        ScmVote vote = new ScmCuratorVote(TestCuratorVote.zookeeperUrl, TestCuratorVote.leaderPath,
                id, new RetryLeader(), new RetryNotLeader());

        vote.startVote();
        waitToLeader(vote);
    }

    private void waitToLeader(ScmVote vote) throws InterruptedException {
        while (!vote.isLeader()) {
            Thread.sleep(1000);
        }
    }
}
