package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionInfo;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description: SCM-1484 :: 查询指定用户下所有会话
 * @author fanyu
 * @Date:2018年5月18日
 * @version:1.0
 */
public class AuthServer_GetUserSession1484 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession session;
    private List< ScmSession > ssList = new ArrayList< ScmSession >();
    private int ssNum = 10;
    private String username = "GetUserSession1484";
    private String passwd = "1484";
    private ScmUser user;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            site = ScmInfo.getSite();
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            for ( int i = 0; i < ssNum; i++ ) {
                ScmSession session = TestScmTools.createSession( site, username,
                        passwd );
                ssList.add( session );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ScmCursor< ScmSessionInfo > cursor = null;
        try {
            cursor = ScmFactory.Session.listSessions( session, username );
            int count = 0;
            while ( cursor.hasNext() ) {
                ScmSessionInfo ssInfo = cursor.getNext();
                Assert.assertEquals( ssInfo.getUsername(), username );
                count++;
            }
            Assert.assertEquals( count, ssNum );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, user );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}