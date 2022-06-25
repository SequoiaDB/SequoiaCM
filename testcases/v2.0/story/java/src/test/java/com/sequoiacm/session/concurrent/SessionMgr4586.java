package com.sequoiacm.session.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @descreption SCM-4586:并发创建session池
 * @author YiPan
 * @date 2022/6/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4586 extends TestScmBase {
    private ScmSessionPoolConf scmSessionPoolConf;

    @BeforeClass
    private void setUp() throws ScmException {
        ScmConfigOption scmConfigOption = TestScmTools
                .getScmConfigOption( ScmInfo.getRootSite().getSiteName() );
        scmSessionPoolConf = ScmSessionPoolConf.builder()
                .setSessionConfig( scmConfigOption ).get();

    }

    @Test
    private void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < 10; i++ ) {
            te.addWorker( new CreateSessionMgr() );
        }
        te.run();
    }

    @AfterClass
    private void tearDown() {
    }

    private class CreateSessionMgr {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSessionMgr sessionMgr = ScmFactory.Session
                    .createSessionMgr( scmSessionPoolConf );
            checkSessionMgr( sessionMgr );
            sessionMgr.close();
        }
    }

    private void checkSessionMgr( ScmSessionMgr sessionMgr )
            throws ScmException {
        // 获取认证session
        ScmSession auth_session = sessionMgr
                .getSession( ScmType.SessionType.AUTH_SESSION );
        try {
            long count = ScmFactory.Workspace.count( auth_session,
                    new BasicBSONObject() );
            Assert.assertNotEquals( count, 0 );
        } finally {
            auth_session.close();
        }

        // 获取非认证session
        ScmSession no_auth_session = sessionMgr
                .getSession( ScmType.SessionType.NOT_AUTH_SESSION );
        try {
            List< String > serviceList = ScmSystem.ServiceCenter
                    .getServiceList( no_auth_session );
            Assert.assertNotEquals( serviceList.size(), 0 );
        } finally {
            no_auth_session.close();
        }
    }
}
