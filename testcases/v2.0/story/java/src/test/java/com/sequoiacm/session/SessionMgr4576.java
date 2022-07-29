package com.sequoiacm.session;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
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
 * @descreption SCM-4576 :: session池已满，将session回池
 * @author Zhaoyujing
 * @Date 2020/6/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4576 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSessionMgr sessionMgr = null;
    private int maxSessionCacheSize = 100;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        sessionMgr = createSessionMgr();
    }

    @Test
    private void test() throws Exception {
        // 生成的第一批session，close后回池，在有效期内仍可获得
        List< ScmSession > sessionList1st = new ArrayList<>();
        // 生成的第二批session，close时session池已满，释放
        List< ScmSession > sessionList2st = new ArrayList<>();
        // 第三批session，从session池获得，和第一批获得的session一致
        List< ScmSession > sessionList3st = new ArrayList<>();

        for ( int i = 0; i < maxSessionCacheSize; i++ ) {
            sessionList1st.add(
                    sessionMgr.getSession( ScmType.SessionType.AUTH_SESSION ) );
        }

        for ( int i = 0; i < maxSessionCacheSize; i++ ) {
            sessionList2st.add(
                    sessionMgr.getSession( ScmType.SessionType.AUTH_SESSION ) );
        }

        for ( ScmSession session : sessionList1st ) {
            session.close();
        }
        for ( ScmSession session : sessionList2st ) {
            session.close();
        }

        for ( int i = 0; i < maxSessionCacheSize; i++ ) {
            ScmSession session = sessionMgr
                    .getSession( ScmType.SessionType.AUTH_SESSION );
            sessionList3st.add( session );
            Assert.assertEquals( sessionList2st.indexOf( session ), -1 );
        }

        Assert.assertEqualsNoOrder( sessionList1st.toArray(),
                sessionList3st.toArray() );

        for ( ScmSession session : sessionList3st ) {
            session.close();
        }
    }

    @AfterClass
    private void tearDown() {
        if ( sessionMgr != null ) {
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
                .setSessionConfig( scOpt )
                .setMaxCacheSize( maxSessionCacheSize ).get();
        ScmSessionMgr sessionMgr = ScmFactory.Session
                .createSessionMgr( sessionPoolConf );

        return sessionMgr;
    }
}
