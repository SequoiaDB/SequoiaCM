package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.exception.ScmError;
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

public class AuthWs_dir2164 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthWs_dir2164.class );
    private static final String NAME = "AuthWs_dir2164";
    private static final String PASSWORD = NAME;
    private static final String[] DIR_PATH_ARRAY = { "/" + NAME + "_a/",
            "/" + NAME + "_a/" + NAME + "_b1/",
            "/" + NAME + "_a/" + NAME + "_b2/",
            "/" + NAME + "_a/" + NAME + "_b3/",
            "/" + NAME + "_a/" + NAME + "_b3/" + NAME + "_c4/",
            "/" + NAME + "_a/" + NAME + "_b3/" + NAME + "_c5/" };
    private static final String[] targetDIR_PATH_ARRAY = {
            "/" + NAME + "_a/" + NAME + "_b4/",
            "/" + NAME + "_a/" + NAME + "e/" + NAME + "g/",
            "/" + NAME + "_a/" + NAME + "_b3/" + NAME + "_c4/" };
    private static ScmRole role = null;
    private static List< ScmResource > resources = new ArrayList<>();
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass
    private void setUp() throws Exception {
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
            deleteDir( ws, DIR_PATH_ARRAY[ i ] );
        }

        for ( int i = targetDIR_PATH_ARRAY.length - 1; i >= 0; i-- ) {
            deleteDir( ws, targetDIR_PATH_ARRAY[ i ] );
        }

        // prepare multiple director
        for ( int i = 0; i < DIR_PATH_ARRAY.length; i++ ) {
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

        ScmFactory.Role.grantPrivilege( session, role, resources.get( 0 ),
                ScmPrivilegeType.CREATE );
        for ( int i = 0; i < DIR_PATH_ARRAY.length - 1; i++ ) {
            ScmFactory.Role.grantPrivilege( session, role, resources.get( i ),
                    ScmPrivilegeType.READ );
        }
        ScmFactory.Role.grantPrivilege( session, role, resources.get( 4 ),
                ScmPrivilegeType.DELETE );

        ScmAuthUtils.checkPriority( site, NAME, NAME, role, wsp.getName() );
    }

    @Test // jira-316
    private void test1() throws Exception {
        // operation business
        ScmSession tSS = null;
        try {
            tSS = TestScmTools.createSession( site, NAME, PASSWORD );
            ScmWorkspace tWS = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    tSS );

            String path = targetDIR_PATH_ARRAY[ 0 ];
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

    @Test // jira-316
    private void test2() throws Exception {
        // operation business
        ScmDirectory dir = null;
        ScmSession tSS = null;
        try {
            tSS = TestScmTools.createSession( site, NAME, PASSWORD );
            ScmWorkspace tWS = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    tSS );

            String path = targetDIR_PATH_ARRAY[ 1 ];
            System.out.println( "path = " + path );
            ScmFactory.Directory.createInstance( tWS,
                    "/" + NAME + "_a/" + NAME + "e/" );
            dir = ScmFactory.Directory.createInstance( tWS, path );
            ScmDirectory rcPath = ScmFactory.Directory.getInstance( tWS, path );
            Assert.assertEquals( rcPath.getPath(), path );
        } finally {
            if ( null != tSS ) {
                tSS.close();
            }
            if ( dir != null ) {
                ScmFactory.Directory.deleteInstance( ws,
                        targetDIR_PATH_ARRAY[ 1 ] );
                ScmFactory.Directory.deleteInstance( ws,
                        "/" + NAME + "_a/" + NAME + "e/" );
            }
        }
        runSuccess = true;
    }

    @Test // jira-316
    private void test3() throws Exception {
        // operation business
        ScmSession tSS = null;
        try {
            tSS = TestScmTools.createSession( site, NAME, PASSWORD );
            ScmWorkspace tWS = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    tSS );

            String path = targetDIR_PATH_ARRAY[ 2 ];
            System.out.println( "path = " + path );
            ScmFactory.Directory.deleteInstance( tWS, path );
            try {
                ScmFactory.Directory.getInstance( tWS, path );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(), ScmError.DIR_NOT_FOUND,
                        e.getMessage() );
            }
        } finally {
            if ( null != tSS ) {
                tSS.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws InterruptedException, ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.revokePrivilege( session, role,
                        resources.get( 0 ), ScmPrivilegeType.CREATE );
                for ( int i = 1; i < DIR_PATH_ARRAY.length - 1; i++ ) {
                    if ( i != 4 ) {
                        ScmFactory.Role.revokePrivilege( session, role,
                                resources.get( i ), ScmPrivilegeType.READ );
                    }
                }
                ScmFactory.User.deleteUser( session, NAME );
                ScmFactory.Role.deleteRole( session, NAME );

                for ( int i = DIR_PATH_ARRAY.length - 1; i >= 0; i-- ) {
                    deleteDir( ws, DIR_PATH_ARRAY[ i ] );
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

    private void deleteDir( ScmWorkspace ws, String dirPath ) {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_NOT_EMPTY ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private List< String > getSubPaths( String path ) {
        String ele = "/";
        String[] arry = path.split( "/" );
        List< String > pathList = new ArrayList< String >();
        for ( int i = 1; i < arry.length; i++ ) {
            ele = ele + arry[ i ];
            pathList.add( ele );
            ele = ele + "/";
        }
        return pathList;
    }
}
