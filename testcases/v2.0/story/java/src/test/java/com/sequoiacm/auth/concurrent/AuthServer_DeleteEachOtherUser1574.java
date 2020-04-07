package com.sequoiacm.auth.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @Description:SCM-1574 :: 多用户并发互相删除对方
 * @author fanyu
 * @Date:2018年5月22日
 * @version:1.0
 */
public class AuthServer_DeleteEachOtherUser1574 extends TestScmBase {
    private boolean runSuccess;
    private SiteWrapper site;
    private ScmSession session;
    private int userNum = 3;
    private String username = "DeleteEachOtherUser1574";
    private String passwd = "1574";
    private List< ScmUser > userList = new CopyOnWriteArrayList< ScmUser >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            for ( int i = 0; i < userNum; i++ ) {
                try {
                    ScmFactory.User.deleteUser( session, username + "_" + i );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                        e.printStackTrace();
                        Assert.fail( e.getMessage() );
                    }
                }
            }
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmUser superUser = ScmFactory.User
                    .getUser( session, TestScmBase.scmUserName );
            Collection< ScmRole > superRoles = superUser.getRoles();
            modifier.addRoles( superRoles );
            for ( int i = 0; i < userNum; i++ ) {
                ScmUser user = ScmFactory.User
                        .createUser( session, username + "_" + i,
                                ScmUserPasswordType.LOCAL,
                                passwd );
                ScmUser user1 = ScmFactory.User
                        .alterUser( session, user, modifier );
                userList.add( user1 );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        DeleteEachOther dThread1 = new DeleteEachOther( userList.get( 0 ),
                userList.get( 1 ) );
        DeleteEachOther dThread2 = new DeleteEachOther( userList.get( 1 ),
                userList.get( 2 ) );
        DeleteEachOther dThread3 = new DeleteEachOther( userList.get( 2 ),
                userList.get( 0 ) );
        dThread1.start();
        dThread2.start();
        dThread3.start();
        boolean dflag1 = dThread1.isSuccess();
        boolean dflag2 = dThread2.isSuccess();
        boolean dflag3 = dThread3.isSuccess();
        Assert.assertEquals( dflag1, true, dThread1.getErrorMsg() );
        Assert.assertEquals( dflag2, true, dThread2.getErrorMsg() );
        Assert.assertEquals( dflag3, true, dThread3.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < userNum; i++ ) {
                    try {
                        ScmFactory.User
                                .deleteUser( session, userList.get( i ) );
                    } catch ( ScmException e ) {
                        if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                            e.printStackTrace();
                            Assert.fail( e.getMessage() );
                        }
                    }
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class DeleteEachOther extends TestThreadBase {
        private ScmUser srcUser;
        private ScmUser dstUser;

        public DeleteEachOther( ScmUser srcUser, ScmUser dstUser ) {
            super();
            this.srcUser = srcUser;
            this.dstUser = dstUser;
        }

        @Override
        public void exec() {
            ScmSession session = null;
            try {
                session = TestScmTools
                        .createSession( site, this.srcUser.getUsername(),
                                passwd );
                ScmFactory.User.deleteUser( session, dstUser.getUsername() );
                check( dstUser );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_BAD_REQUEST &&
                        e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR
                        && e.getError() != ScmError.HTTP_UNAUTHORIZED &&
                        e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
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
