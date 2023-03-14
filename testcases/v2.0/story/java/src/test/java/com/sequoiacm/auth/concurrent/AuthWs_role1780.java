package com.sequoiacm.auth.concurrent;

import java.util.Random;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1780:删除角色
 * @Author huangxioni
 * @Date 2018/6/7
 */

public class AuthWs_role1780 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthWs_role1780.class );
    private static final String NAME = "authws1780";
    private static final String PASSWORD = NAME;
    private static final String DIR_PATH = "/" + NAME;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmRole role = null;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        // clean users and roles
        try {
            ScmFactory.User.deleteUser( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean users in setUp, errorMsg = [" + e.getError() + "]" );
        }
        try {
            ScmFactory.Role.deleteRole( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean roles in setUp, errorMsg = [" + e.getError() + "]" );
        }

        // clean director
        try {
            ScmFactory.Directory.deleteInstance( ws, DIR_PATH );
        } catch ( ScmException e ) {
            logger.info( "Directory does not exist, errorMsg = [" + e.getError()
                    + "]" );
        }

        // prepare user
        this.createUserAndRole();

        // prepare director
        ScmFactory.Directory.createInstance( ws, DIR_PATH );
    }

    @Test
    private void test() throws ScmException, InterruptedException {
        PrivilegeRole privRole = new PrivilegeRole();
        DeleteRole delRole = new DeleteRole();
        Random random = new Random();
        privRole.start( random.nextInt( 10 ) + 1 );
        delRole.start( random.nextInt( 10 ) + 1 );
        if ( !( privRole.isSuccess() && delRole.isSuccess() ) ) {
            Assert.fail( privRole.getErrorMsg() + delRole.getErrorMsg() );
        }

        // check privilege
        ScmCursor< ScmPrivilege > priCursor = ScmFactory.Privilege
                .listPrivileges( session, role );

        while ( priCursor.hasNext() ) {
            ScmPrivilege pri = priCursor.getNext();
            logger.info( "priority = " + pri.getPrivilegeType() + ",resource = "
                    + pri.getResourceId() );
        }
        // Assert.assertFalse(priCursor.hasNext());

        // check role
        try {
            ScmFactory.Role.getRole( session, NAME );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "get not exist role, errorMsg = [" + e.getError() + "]" );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, NAME );
                try {
                    ScmFactory.Role.deleteRole( session, NAME );
                } catch ( ScmException e ) {
                    logger.info(
                            "delete not eixst role in tear down, errorMsg = "
                                    + e.getError() );
                }
                ScmFactory.Directory.deleteInstance( ws, DIR_PATH );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createUserAndRole() throws ScmException {
        ScmUser scmUser = ScmFactory.User.createUser( session, NAME,
                ScmUserPasswordType.LOCAL, PASSWORD );
        role = ScmFactory.Role.createRole( session, NAME, "" );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    private class PrivilegeRole extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmResource resource = ScmResourceFactory
                        .createDirectoryResource( wsp.getName(), DIR_PATH );
                ScmFactory.Role.grantPrivilege( session, role, resource,
                        ScmPrivilegeType.ALL );
            } catch ( ScmException e ) {
                if ( ScmError.PRIVILEGE_GRANT_FAILED != e.getError() ) {
                    e.printStackTrace();
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteRole extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmFactory.Role.deleteRole( session, NAME );
            } catch ( ScmException e ) {
                if ( ScmError.HTTP_NOT_FOUND != e.getError() ) {
                    e.printStackTrace();
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
