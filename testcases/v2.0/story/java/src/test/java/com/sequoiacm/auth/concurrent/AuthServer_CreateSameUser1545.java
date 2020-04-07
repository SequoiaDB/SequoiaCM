package com.sequoiacm.auth.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @Description:SCM-1545 :: 并发创建相同用户
 * @author fanyu
 * @Date:2018年5月19日
 * @version:1.0
 */
public class AuthServer_CreateSameUser1545 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession session;
    private String username = "CreateSameUser1545";
    private String passwd = "1545";
    private AtomicInteger atom = new AtomicInteger( 1 );

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
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        CreateSameUser cThread = new CreateSameUser();
        cThread.start( 30 );
        boolean cflag = cThread.isSuccess();
        Assert.assertEquals( cflag, true, cThread.getErrorMsg() );
        Assert.assertEquals( atom.get(), 1, atom.get() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, username );
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

    private class CreateSameUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmUser user = ScmFactory.User.createUser( session, username,
                        ScmUserPasswordType.LOCAL, passwd );
                check( user );
                atom.getAndIncrement();
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_BAD_REQUEST
                        &&
                        e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }

        private void check( ScmUser expUser ) {
            try {
                ScmUser actUser = ScmFactory.User.getUser( session, username );
                Assert.assertEquals( actUser.getRoles().size(), 0,
                        actUser.toString() );
                Assert.assertEquals( actUser.getUserId(), expUser.getUserId(),
                        actUser.toString() );
                Assert.assertEquals( actUser.getUsername(),
                        expUser.getUsername(), actUser.toString() );
                Assert.assertEquals( actUser.getPasswordType(),
                        expUser.getPasswordType(), actUser.toString() );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }
}
