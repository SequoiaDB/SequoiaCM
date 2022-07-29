package com.sequoiacm.session;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4571 :: session池中session已过期，获取session
 * @descreption SCM-4582 :: 用户创建session池，指定session存活周期
 * @author Zhaoyujing
 * @Date 2020/6/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4571_4582 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSessionMgr sessionMgr = null;
    private int timeOutSecond = 10;
    private ScmSessionPoolConf sessionPoolConf = null;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();

        List< String > urlList = new ArrayList<>();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + site.getSiteServiceName() );
        }
        ScmConfigOption scOpt = new ScmConfigOption( urlList,
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        sessionPoolConf = ScmSessionPoolConf.builder().setSessionConfig( scOpt )
                .get();
    }

    @Test
    private void test() throws Exception {
        try {
            sessionPoolConf.setKeepAliveTime( 0 );
            ScmFactory.Session.createSessionMgr( sessionPoolConf );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.INVALID_ARGUMENT.getErrorCode() );
        }
        try {
            sessionPoolConf.setKeepAliveTime( -1 );
            ScmFactory.Session.createSessionMgr( sessionPoolConf );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.INVALID_ARGUMENT.getErrorCode() );
        }

        sessionPoolConf.setKeepAliveTime( timeOutSecond );
        sessionMgr = ScmFactory.Session.createSessionMgr( sessionPoolConf );

        // 获取session，回池
        ScmSession session1st = sessionMgr
                .getSession( ScmType.SessionType.AUTH_SESSION );
        String oldSessionId = session1st.getSessionId();
        session1st.close();

        // 有效期内再次获得session，是池内的session，回池
        ScmSession session2st = sessionMgr
                .getSession( ScmType.SessionType.AUTH_SESSION );
        Assert.assertEquals( session2st.getSessionId(), oldSessionId );
        session2st.close();

        Thread.sleep( timeOutSecond * 1000 );

        // 超时后再次获得session，不是原池内的session，回池
        ScmSession session3st = sessionMgr
                .getSession( ScmType.SessionType.AUTH_SESSION );
        Assert.assertNotEquals( session3st.getSessionId(), oldSessionId );
        session3st.close();
    }

    @AfterClass
    private void tearDown() {
        if ( sessionMgr != null ) {
            sessionMgr.close();
        }
    }
}
