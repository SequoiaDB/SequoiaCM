package com.sequoiacm.session.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4590 :: 并发关闭和获取session
 * @author Zhaoyujing
 * @Date 2020/6/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4590 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSessionMgr sessionMgr = null;

    @BeforeClass
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test
    private void test() throws Exception {
        sessionMgr = createSessionMgr();

        ThreadExecutor teSession = new ThreadExecutor( );
        GetSession t1 = new GetSession();
        CloseSessionMgr t2 = new CloseSessionMgr();
        teSession.addWorker( t1 );
        teSession.addWorker( t2 );
        teSession.run();
    }

    @AfterClass
    private void tearDown() {
        if ( sessionMgr != null ) {
            sessionMgr.close();
        }
    }

    class GetSession {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmSession session = sessionMgr.getSession();
                ScmCursor< ScmBucket > cursor = ScmFactory.Bucket
                        .listBucket( session, null, null, 0, -1 );
                cursor.close();
                session.close();
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getErrorCode(),
                        ScmError.OPERATION_UNSUPPORTED.getErrorCode() );
            }
        }
    }

    class CloseSessionMgr {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            sessionMgr.close();
        }
    }

    private ScmSessionMgr createSessionMgr() throws ScmException {
        List< String > urlList = new ArrayList<>();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + site.getSiteServiceName() );
        }
        ScmConfigOption scOpt = new ScmConfigOption( urlList,
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        ScmSessionPoolConf sessionPoolConf = ScmSessionPoolConf.builder()
                .setSessionConfig( scOpt ).get();
        ScmSessionMgr sessionMgr = ScmFactory.Session
                .createSessionMgr( sessionPoolConf );

        return sessionMgr;
    }
}
