package com.sequoiacm.auth;

import java.io.File;

import org.apache.log4j.Logger;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;

/**
 * @FileName SCM-1492:创建admin用户[创建用户，添加AUTH_ADMIN角色]
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1492 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1492.class );
    private static final String NAME = "auth1492";
    private static final String PASSWORD = NAME;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmRole authAdminRole = null;

    private int fileSize = 10;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            session = ScmSessionUtils.createSession( site );

            // clean new user
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

            // get AUTH_ADMIN role
            ScmUser adminUser = ScmFactory.User.getUser( session,
                    TestScmBase.scmUserName );
            authAdminRole = adminUser.getRoles().iterator().next();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_createUserAndRole()
            throws ScmException, InterruptedException {
        // create ScmUser
        ScmUser scmUser = ScmFactory.User.createUser( session, NAME,
                ScmUserPasswordType.LOCAL, PASSWORD );

        // add AUTH_ADMIN role
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( authAdminRole );
        ScmUser mUser = ScmFactory.User.alterUser( session, scmUser, modifier );

        // check results
        scmUser = ScmFactory.User.getUser( session, NAME );
        Assert.assertEquals( mUser.getRoles().size(), 1 );
        Assert.assertEquals( mUser.getRoles().iterator().next(),
                authAdminRole );
        Assert.assertEquals( mUser.getRoles().iterator().next().getRoleName(),
                "ROLE_AUTH_ADMIN" );
        Assert.assertEquals(
                mUser.getRoles().iterator().next().getDescription(),
                "authentication administrator" );
        Assert.assertNotNull( mUser.getRoles().iterator().next().getRoleId() );

        // delete user
        ScmFactory.User.deleteUser( session, scmUser );
        try {
            ScmFactory.User.getUser( session, NAME );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "get user after delete, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

}
