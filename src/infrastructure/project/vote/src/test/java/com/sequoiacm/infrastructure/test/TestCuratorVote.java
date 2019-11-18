package com.sequoiacm.infrastructure.test;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.infrastructure.vote.ScmLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmNotLeaderAction;
import com.sequoiacm.infrastructure.vote.ScmVote;
import com.sequoiacm.infrastructure.vote.curator.ScmCuratorVote;

class ScmTestEvent {
    public static final String TYPE_LEADER = "leader";
    public static final String TYPE_NOLEADER = "noleader";
    public static final String TYPE_QUIT = "quit";
    private String type;
    private String id;
    public ScmTestEvent(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id + ":" + type;
    }
}

class ScmTestEventRecorder {
    private List<ScmTestEvent> eventList = new ArrayList<ScmTestEvent>();
    private int count = 0;
    public ScmTestEventRecorder() {
    }

    public void addEvent(ScmTestEvent event) {
        count++;    //多线程重入时，count计数错误
        eventList.add(event);
    }

    public List<ScmTestEvent> getEventList() {
        return eventList;
    }

    public int getCheckCount() {
        return count;
    }
}

class MyLeaderAction implements ScmLeaderAction {
    private ScmTestEventRecorder recorder;
    private String id;
    public MyLeaderAction(ScmTestEventRecorder recorder, String id) {
        this.recorder = recorder;
        this.id = id;
    }

    @Override
    public void run() {
        recorder.addEvent(new ScmTestEvent(ScmTestEvent.TYPE_LEADER, id));
    }
}

class MyNotLeaderAction implements ScmNotLeaderAction {
    private ScmTestEventRecorder recorder;
    private String id;
    public MyNotLeaderAction(ScmTestEventRecorder recorder, String id) {
        this.recorder = recorder;
        this.id = id;
    }

    @Override
    public void run() {
        recorder.addEvent(new ScmTestEvent(ScmTestEvent.TYPE_NOLEADER, id));
    }
}

class VoteTestThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(VoteTestThread.class);

    private String id;
    private ScmTestEventRecorder recorder;
    public VoteTestThread(String id, ScmTestEventRecorder recorder) {
        this.id = id;
        this.recorder = recorder;
    }

    @Override
    public void run() {
        ScmVote vote = null;
        try {
            vote = new ScmCuratorVote(TestCuratorVote.zookeeperUrl,
                    TestCuratorVote.leaderPath, id,
                    new MyLeaderAction(recorder, id), new MyNotLeaderAction(recorder, id));

            vote.startVote();
            waitToLeader(vote);

            recorder.addEvent(new ScmTestEvent(ScmTestEvent.TYPE_QUIT, id));
            vote.close();
        }
        catch (Exception e) {
            logger.error("run VoteTestThread failed", e);
        }
        finally {
            if (vote != null) {
                vote.close();
            }
        }
    }

    private void waitToLeader(ScmVote vote) throws InterruptedException {
        while (!vote.isLeader()) {
            Thread.sleep(1000);
        }
    }
}


public class TestCuratorVote {
    private static final Logger logger = LoggerFactory.getLogger(TestCuratorVote.class);

    public static final String zookeeperUrl = "192.168.20.56:2181";
    public static final String leaderPath = "/scm/schedule/leader";

    @BeforeClass
    public static void setUp() {

    }

    @Test
    public void TestLeader() throws Exception {
        ScmTestEventRecorder recorder = new ScmTestEventRecorder();
        try {
            //            String id = "1";
            //            ScmVote vote1 = new ScmCuratorVote(TestCuratorVote.zookeeperUrl,
            //                    TestCuratorVote.leaderPath, id,
            //                    new MyLeaderAction(recorder, id), new MyNotLeaderAction(recorder, id));
            //
            //            id = "2";
            //            ScmVote vote2 = new ScmCuratorVote(TestCuratorVote.zookeeperUrl,
            //                    TestCuratorVote.leaderPath, id,
            //                    new MyLeaderAction(recorder, id), new MyNotLeaderAction(recorder, id));
            //
            //            vote1.startVote();
            //            vote2.startVote();
            //
            //            Thread.sleep(10000);

            List<VoteTestThread> tList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tList.add(new VoteTestThread(i + "", recorder));
            }

            for (VoteTestThread t : tList) {
                t.start();
            }

            for (VoteTestThread t : tList) {
                t.join();
            }

            Assert.assertEquals(recorder.getCheckCount(), recorder.getEventList().size());

            String checkId = null;
            for (ScmTestEvent e : recorder.getEventList()) {
                logger.info(e.toString());
                if (checkId == null) {
                    checkId = e.getId();
                    Assert.assertEquals(e.getType(), ScmTestEvent.TYPE_LEADER);
                }
                else {
                    Assert.assertEquals(e.getId(), checkId);
                    Assert.assertEquals(e.getType(), ScmTestEvent.TYPE_QUIT);
                    checkId = null;
                }
            }
        }
        finally {

        }
    }

    @AfterClass
    public static void tearDown() {

    }
}
