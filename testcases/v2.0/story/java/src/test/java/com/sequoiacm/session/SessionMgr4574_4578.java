package com.sequoiacm.session;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4578 :: 用户创建session池，指定最大连接数
 * @descreption SCM-4574 :: session池最大连接数达到上限，再次使用session发送请求
 * @author Zhaoyujing
 * @Date 2020/6/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4574_4578 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSessionMgr sessionMgr = null;
    private int maxConnections = 300;
    private ScmConfigOption scOpt = null;
    private ScmSessionPoolConf sessionPoolConf = null;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();

        List< String > urlList = new ArrayList<>();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + site.getSiteServiceName() );
        }
        scOpt = new ScmConfigOption( urlList, TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        sessionPoolConf = ScmSessionPoolConf.builder().setSessionConfig( scOpt )
                .get();
    }

    @Test
    private void test() throws Exception {
        try {
            sessionPoolConf.setMaxConnections( -1 );
            ScmFactory.Session.createSessionMgr( sessionPoolConf );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.INVALID_ARGUMENT.getErrorCode() );
        }

        try {
            sessionPoolConf.setMaxConnections( 0 );
            ScmFactory.Session.createSessionMgr( sessionPoolConf );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.INVALID_ARGUMENT.getErrorCode() );
        }

        sessionPoolConf.setMaxConnections( maxConnections );
        sessionMgr = ScmFactory.Session.createSessionMgr( sessionPoolConf );
        List< ScmSession > sessionList = new ArrayList<>();
        for ( int i = 0; i < maxConnections; i++ ) {
            sessionList.add( sessionMgr.getSession() );
        }

        ScmSession session = sessionMgr.getSession();
        List< ScmCursor > cursors = new ArrayList<>();
        for ( int i = 0; i < maxConnections; i++ ) {
            cursors.add( ScmFactory.Bucket.listBucket( session, null, null, 0,
                    -1 ) );
        }

        try {
            ScmFactory.Bucket.listBucket( session, null, null, 0, -1 );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.NETWORK_IO.getErrorCode() );
        }

        for ( ScmCursor cursor : cursors ) {
            cursor.close();
        }
        session.close();

        for ( ScmSession scmSession : sessionList ) {
            scmSession.close();
        }
    }

    @AfterClass
    private void tearDown() {
        if ( sessionMgr != null ) {
            sessionMgr.close();
        }
    }
}
