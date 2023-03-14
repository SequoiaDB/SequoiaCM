package com.sequoiacm.auth.concurrent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @Description: SCM-1548 :: 并发删除不同用户
 * @author fanyu
 * @Date:2018年5月21日
 * @version:1.0
 */
public class AuthServer_DeleteDiffUser1548 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;
    private int userNum = 10;
    private String username = "DeleteDiffUser1548";
    private String passwd = "1548";
    private List< ScmUser > userList = new CopyOnWriteArrayList< ScmUser >();
    private AtomicInteger atom = new AtomicInteger( 0 );

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = ScmSessionUtils.createSession( site );
            site = ScmInfo.getSite();
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            for ( int i = 0; i < userNum; i++ ) {
                ScmUser user = ScmFactory.User.createUser( session,
                        username + "_" + i, ScmUserPasswordType.LOCAL, passwd );
                userList.add( user );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        DeleteSameUser dThread = new DeleteSameUser();
        dThread.start( 10 );
        boolean dflag = dThread.isSuccess();
        Assert.assertEquals( dflag, true, dThread.getErrorMsg() );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }

    private class DeleteSameUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                int index = atom.getAndIncrement();
                ScmUser user = userList.get( index );
                ScmFactory.User.deleteUser( session, user );
                check( user );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( ScmUser user ) {
            try {
                ScmFactory.User.getUser( session, user.getUsername() );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }
}
