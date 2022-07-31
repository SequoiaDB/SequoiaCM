package com.sequoiacm.session.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4587 :: 并发关闭 session池
 * @descreption SCM-4588 :: 并发获取session
 * @author Zhaoyujing
 * @Date 2020/6/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4587_4588 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSessionMgr sessionMgrA = null;
    private ScmSessionMgr sessionMgrB = null;

    @BeforeClass
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test
    private void test() throws Exception {
        sessionMgrA = createSessionMgr();
        sessionMgrB = createSessionMgr();

        ThreadExecutor teSession = new ThreadExecutor( );
        GetSession t1 = new GetSession( ScmType.SessionType.AUTH_SESSION );
        GetSession t2 = new GetSession( ScmType.SessionType.AUTH_SESSION );
        GetSession t3 = new GetSession( ScmType.SessionType.NOT_AUTH_SESSION );
        teSession.addWorker( t1 );
        teSession.addWorker( t2 );
        teSession.addWorker( t3 );
        teSession.run();

        ThreadExecutor teSessionMgr = new ThreadExecutor( );
        CloseSessionMgr t4 = new CloseSessionMgr( sessionMgrA );
        CloseSessionMgr t5 = new CloseSessionMgr( sessionMgrA );
        CloseSessionMgr t6 = new CloseSessionMgr( sessionMgrB );
        teSessionMgr.addWorker( t4 );
        teSessionMgr.addWorker( t5 );
        teSessionMgr.addWorker( t6 );
        teSessionMgr.run();
    }

    @AfterClass
    private void tearDown() {
        if ( sessionMgrA != null ) {
            sessionMgrA.close();
        }
        if ( sessionMgrB != null ) {
            sessionMgrB.close();
        }
    }

    class GetSession {
        ScmType.SessionType type;

        GetSession( ScmType.SessionType type ) {
            this.type = type;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmSession session = sessionMgrA.getSession( type );
            if ( type.equals( ScmType.SessionType.AUTH_SESSION ) ) {
                ScmCursor< ScmBucket > cursor = ScmFactory.Bucket
                        .listBucket( session, null, null, 0, -1 );
                cursor.close();
            } else {
                ScmSystem.ServiceCenter.getServiceList( session );
            }
            session.close();
        }
    }

    class CloseSessionMgr {
        private ScmSessionMgr sessionMgr;

        CloseSessionMgr( ScmSessionMgr sessionMgr ) {
            this.sessionMgr = sessionMgr;
        }

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
