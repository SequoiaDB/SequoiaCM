package com.sequoiacm.session.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4589:并发归还session
 * @author YiPan
 * @date 2022/6/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4589 extends TestScmBase {
    private ScmSessionMgr sessionMgr;
    private List< String > expSessionIds = new ArrayList<>();
    private List< String > actSessionIds = new ArrayList<>();

    @BeforeClass
    private void setUp() throws ScmException {
        ScmConfigOption scmConfigOption = ScmSessionUtils
                .getScmConfigOption( ScmInfo.getRootSite().getSiteName() );
        ScmSessionPoolConf scmSessionPoolConf = ScmSessionPoolConf.builder()
                .setSessionConfig( scmConfigOption ).get();
        sessionMgr = ScmFactory.Session.createSessionMgr( scmSessionPoolConf );
    }

    @Test
    private void test() throws Exception {
        // 获取两个session
        ScmSession session = sessionMgr.getSession();
        ScmSession diffSession = sessionMgr.getSession();
        expSessionIds.add( session.getSessionId() );
        expSessionIds.add( diffSession.getSessionId() );

        // 并发归还
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < 5; i++ ) {
            te.addWorker( new Release( session ) );
        }
        for ( int i = 0; i < 5; i++ ) {
            te.addWorker( new Release( diffSession ) );
        }
        te.run();

        // 再次获取
        ScmSession session1 = sessionMgr.getSession();
        actSessionIds.add( session1.getSessionId() );
        ScmSession session2 = sessionMgr.getSession();
        actSessionIds.add( session2.getSessionId() );
        session1.close();
        session2.close();

        // 校验获取到的sessionId是否相同
        Assert.assertEqualsNoOrder( actSessionIds.toArray(),
                expSessionIds.toArray() );
    }

    @AfterClass
    private void tearDown() {
        sessionMgr.close();
    }

    private class Release {
        private ScmSession session;

        public Release( ScmSession session ) {
            this.session = session;
        }

        @ExecuteOrder(step = 1)
        private void run() {
            session.close();
        }
    }
}
