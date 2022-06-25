package com.sequoiacm.session;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @descreption SCM-4568:session池中存在session，获取不同类型的session使用
 * @author YiPan
 * @date 2022/6/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4568 extends TestScmBase {
    private ScmSessionMgr sessionMgr;

    @BeforeClass
    private void setUp() throws ScmException {
        ScmInfo.getRootSite();
        sessionMgr = TestScmTools.createSessionMgr( ScmInfo.getRootSite() );
    }

    @Test
    private void test() throws ScmException {
        // 获取认证session
        ScmSession auth_session = sessionMgr
                .getSession( ScmType.SessionType.AUTH_SESSION );
        ScmWorkspace workspace = ScmFactory.Workspace
                .getWorkspace( ScmInfo.getWs().getName(), auth_session );
        Assert.assertNotNull( workspace );

        // 获取非认证session
        ScmSession no_auth_session = sessionMgr
                .getSession( ScmType.SessionType.NOT_AUTH_SESSION );
        List< String > serviceList = ScmSystem.ServiceCenter
                .getServiceList( no_auth_session );
        Assert.assertNotEquals( serviceList.size(), 0 );

        // 释放session回池
        auth_session.close();
        no_auth_session.close();

        // 再次获取使用
        auth_session = sessionMgr
                .getSession( ScmType.SessionType.AUTH_SESSION );
        workspace = ScmFactory.Workspace
                .getWorkspace( ScmInfo.getWs().getName(), auth_session );
        Assert.assertNotNull( workspace );

        no_auth_session = sessionMgr
                .getSession( ScmType.SessionType.NOT_AUTH_SESSION );
        serviceList = ScmSystem.ServiceCenter.getServiceList( no_auth_session );
        Assert.assertNotEquals( serviceList.size(), 0 );
        //回池
        auth_session.close();
        no_auth_session.close();
    }

    @AfterClass
    private void tearDown() {
        sessionMgr.close();
    }
}
