package com.sequoiacm.auth;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionInfo;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1481 :: 用户下存在多个会话，登出
 * @author fanyu
 * @Date:2018年5月18日
 * @version:1.0
 */
public class AuthServer_LogoutOnManySession1141 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmWorkspace wsA;
    private ScmSession session;
    private String username = "LogoutOnManySession1141";
    private String rolename = "LogoutOnManySession1141_r";
    private String passwd = "1141";
    private ScmUser user;
    private List< ScmSession > sessionList = new CopyOnWriteArrayList< ScmSession >();
    private List< ScmId > fileIdList = new CopyOnWriteArrayList< ScmId >();
    private String author = "LogoutOnManySession1141";
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws InterruptedException {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        LoginAndDoSomething thread = new LoginAndDoSomething();
        thread.start( 20 );
        boolean flag = thread.isSuccess();
        Assert.assertEquals( flag, true, thread.getErrorMsg() );
        ScmSession session1 = sessionList.remove( ( int ) Math.random() * 10 );
        if ( session1 != null ) {
            try {
                ScmFactory.Session.deleteSession( session,
                        session1.getSessionId() );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } else {
            Assert.fail( "session1 is null" );
        }
        ScmCursor< ScmSessionInfo > cursor = null;
        int count = 0;
        try {
            cursor = ScmFactory.Session.listSessions( session, username );
            if ( cursor == null ) {
                Assert.fail(
                        "exp session cursor is not null but find cursor is "
                                + "null success" );
            }
            while ( cursor.hasNext() ) {
                if ( cursor.getNext().getSessionId()
                        .equals( session1.getSessionId() ) ) {
                    Assert.fail( "sesssion close is fail,session1 = "
                            + session1.toString() );
                }
                count++;
            }
            Assert.assertEquals( count, 19, sessionList.toString() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.deleteRole( session, rolename );
                ScmFactory.User.deleteUser( session, user );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsA, fileId, true );
                }
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

    private void checkPrivilege( SiteWrapper site, WsWrapper wsp )
            throws InterruptedException {
        int i = 0;
        for ( ; i < 60; i++ ) {
            ScmSession session = null;
            try {
                Thread.sleep( 1000 );
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile writefile = ScmFactory.File.createInstance( ws );
                writefile.setFileName( author + "_" + UUID.randomUUID() );
                writefile.setAuthor( author );
                ScmId fileId = writefile.save();
                ScmFactory.File.deleteInstance( ws, fileId, true );
                break;
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED
                        || i == 59 ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() + ",i = " + i );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private void cleanEnv() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        try {
            ScmFactory.Role.deleteRole( session, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepare() throws InterruptedException {
        try {
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            ScmRole role = ScmFactory.Role.createRole( session, rolename,
                    null );
            ScmUserModifier modifier = new ScmUserModifier();
            ScmResource rs = ScmResourceFactory
                    .createWorkspaceResource( wsp.getName() );
            ScmFactory.Role.grantPrivilege( session, role, rs,
                    ScmPrivilegeType.ALL );
            modifier.addRole( role );
            ScmFactory.User.alterUser( session, user, modifier );
            checkPrivilege( site, wsp );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private class LoginAndDoSomething extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmSession session = TestScmTools.createSession( site, username,
                        passwd );
                sessionList.add( session );
                int random = ( int ) Math.random() * 20;
                if ( random % 3 == 0 ) {
                    createFile( session );
                }
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void createFile( ScmSession session ) {
            ScmWorkspace ws = null;
            ScmId fileId = null;
            try {
                ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                        session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( author + "_" + UUID.randomUUID() );
                file.setAuthor( author );
                fileId = file.save();
                fileIdList.add( fileId );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }
}
