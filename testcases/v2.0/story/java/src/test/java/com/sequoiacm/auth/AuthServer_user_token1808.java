package com.sequoiacm.auth;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1808:token认证方式的用户增删改查，密码允许任意值
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user_token1808 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user_token1808.class );
    private static final String NAME = "auth1808";
    private static final String PASSWORD = NAME;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass
    private void setUp() {
        try {
            site = ScmInfo.getRootSite();
            session = ScmSessionUtils.createSession( site );
            wsp = ScmInfo.getWs();

            // clean new user and role
            try {
                ScmFactory.User.deleteUser( session, NAME );
            } catch ( ScmException e ) {
                logger.info( "clean users in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }
            try {
                ScmFactory.Role.deleteRole( session, NAME );
            } catch ( ScmException e ) {
                logger.info( "clean roles in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }
            // clean scmFile
            // BSONObject cond =
            // ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(NAME)
            // .get();
            // ScmFileUtils.cleanFile(wsp, cond);
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test
    private void test() throws ScmException, InterruptedException {
        // create user
        ScmUser user = ScmFactory.User.createUser( session, NAME,
                ScmUserPasswordType.TOKEN, PASSWORD );
        ScmRole role = ScmFactory.Role.createRole( session, NAME, "" );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( NAME );
        ScmFactory.User.alterUser( session, user, modifier );

        user = ScmFactory.User.getUser( session, NAME );
        Assert.assertEquals( user.getUsername(), NAME );
        Assert.assertTrue( user.hasRole( NAME ) );

        // login and operation business
        ScmSession ss = null;
        try {
            ss = ScmSessionUtils.createSession( site, NAME, "test123456" );
            Assert.assertEquals( ss.getUser(), NAME );
            // grant privilege
            ScmResource resource = ScmResourceFactory
                    .createWorkspaceResource( wsp.getName() );
            ScmFactory.Role.grantPrivilege( session, role, resource,
                    ScmPrivilegeType.ALL );

            // operation business
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    ss );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( NAME );
            ScmId fileId = file.save();

            file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getFileName(), NAME );

            ScmFactory.File.deleteInstance( ws, fileId, true );

            // logout
            ss.close();
            try ( ScmCursor< ScmSessionInfo > cursor = ScmFactory.Session
                    .listSessions( session, NAME )) {
                Assert.assertFalse( cursor.hasNext() );
            }
        } finally {
            if ( null != ss )
                ss.close();
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, NAME );
                ScmFactory.Role.deleteRole( session, NAME );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

}
