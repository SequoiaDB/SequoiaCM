package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @FileName SCM-1733:查询到的目录权限精确匹配到中间
 * @Author huangxioni
 * @Date 2018/6/7
 */

public class AuthWs_dir1732 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthWs_dir1732.class );
    private static final String NAME = "authws1732";
    private static final String PASSWORD = NAME;
    private static final String[] DIR_PATH_ARRAY = { "/" + NAME + "_a/",
            "/" + NAME + "_a/" + NAME + "_a1/", "/" + NAME + "_b/",
            "/" + NAME + "_c/", "/" + NAME + "_a/" + NAME + "_b1/" };
    private static ScmRole role = null;
    // private static final String[]
    // DIR_PATH_ARRAY = {"/" + NAME + "_a/",
    // "/" + NAME + "_a/" + NAME + "_a1/",
    // "/" + NAME + "_c/",
    // "/" + NAME + "_c/" + NAME + "_c1/",
    // "/" + NAME + "_c/" + NAME + "_c1/" + NAME + "_c2/",
    // "/" + NAME + "_a/" + NAME + "_b1/"};
    private static List< ScmResource > resources = new ArrayList<>();
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    // private static final String[]
    // DIR_PATH_ARRAY = {"/" + NAME + "_a/",
    // "/" + NAME + "_a/" + NAME + "_a1/",
    // "/" + NAME + "_a/" + NAME + "_b1/"};
    private ScmWorkspace ws = null;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
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
        for ( int i = DIR_PATH_ARRAY.length - 1; i >= 0; i-- ) {
            try {
                System.out.println(
                        "i = " + i + ", dirPath = " + DIR_PATH_ARRAY[ i ] );
                ScmFactory.Directory.deleteInstance( ws, DIR_PATH_ARRAY[ i ] );
            } catch ( ScmException e ) {
                logger.info( "clean dirPath in setUp, errorMsg = ["
                        + e.getError() + "]" );
            }
        }

        // prepare multiple director
        for ( int i = 0; i < DIR_PATH_ARRAY.length - 1; i++ ) {
            ScmFactory.Directory.createInstance( ws, DIR_PATH_ARRAY[ i ] );
        }

        // prepare resource
        for ( int i = 0; i < DIR_PATH_ARRAY.length - 1; i++ ) {
            ScmResource resource = ScmResourceFactory.createDirectoryResource(
                    wsp.getName(), DIR_PATH_ARRAY[ i ] );
            resources.add( resource );
        }

        // prepare user
        this.createUserAndRole();
    }

    @Test(groups = { GroupTags.base }) // jira-316
    private void test() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resources.get( 0 ),
                ScmPrivilegeType.CREATE );
        for ( int i = 0; i < DIR_PATH_ARRAY.length - 1; i++ ) {
            ScmFactory.Role.grantPrivilege( session, role, resources.get( i ),
                    ScmPrivilegeType.READ );
        }

        ScmAuthUtils.checkPriority( site, NAME, NAME, role, wsp.getName() );

        /*
         * ScmCursor<ScmPrivilege> cursor =
         * ScmFactory.Privilege.listPrivileges(session, role); while
         * (cursor.hasNext()) { ScmPrivilege info = cursor.getNext();
         * System.out.println("---"+info.getResource().toStringFormat());
         * System.out.println("---"+info.getResourceId());
         * System.out.println("---"+info.getPrivilege()); System.out.println();
         * }
         */

        // operation business
        ScmSession tSS = null;
        try {
            tSS = TestScmTools.createSession( site, NAME, PASSWORD );
            ScmWorkspace tWS = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    tSS );

            String path = DIR_PATH_ARRAY[ DIR_PATH_ARRAY.length - 1 ];
            System.out.println( "path = " + path );
            ScmFactory.Directory.createInstance( tWS, path );
            ScmDirectory rcPath = ScmFactory.Directory.getInstance( tWS, path );
            Assert.assertEquals( rcPath.getPath(), path );
        } finally {
            if ( null != tSS ) {
                tSS.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException, InterruptedException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.revokePrivilege( session, role,
                        resources.get( 0 ), ScmPrivilegeType.CREATE );
                for ( int i = 1; i < DIR_PATH_ARRAY.length - 1; i++ ) {
                    ScmFactory.Role.revokePrivilege( session, role,
                            resources.get( i ), ScmPrivilegeType.READ );
                }
                ScmFactory.User.deleteUser( session, NAME );
                ScmFactory.Role.deleteRole( session, NAME );

                for ( int i = DIR_PATH_ARRAY.length - 1; i >= 0; i-- ) {
                    ScmFactory.Directory.deleteInstance( ws,
                            DIR_PATH_ARRAY[ i ] );
                }
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
}
