package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
import com.sequoiacm.client.element.privilege.ScmPrivilegeDefine;
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
 * @Description:SCM-1735 1736 1737::
 *                       查询的目录权限最大串匹配到头部,中间,末尾,即目录资源排序的第一位,中间,末尾,中间的左右两边
 * @author fanyu
 * @Date:2018年6月15日
 * @version:1.0
 */
public class AuthWs_Dir1735_To_1737 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmSession sessionUser;
    private ScmWorkspace wsA;
    private ScmWorkspace wsUser;
    private String username = "AuthWs_Dir1735_To_1737";
    private String rolename = "ROLE_1737_D";
    private String passwd = "353637";
    private ScmUser user;
    private ScmRole role;
    private List< ScmResource > dirrsList = new ArrayList< ScmResource >();
    private String[] privileges = { ScmPrivilegeDefine.READ,
            ScmPrivilegeDefine.DELETE,

            ScmPrivilegeDefine.CREATE + "|" + ScmPrivilegeDefine.READ,

            ScmPrivilegeDefine.READ + "|" + ScmPrivilegeDefine.UPDATE,

            ScmPrivilegeDefine.READ + "|" + ScmPrivilegeDefine.DELETE,
            ScmPrivilegeDefine.UPDATE + "|" + ScmPrivilegeDefine.DELETE,

            ScmPrivilegeDefine.CREATE + "|" + ScmPrivilegeDefine.READ + "|"
                    + ScmPrivilegeDefine.UPDATE,

            ScmPrivilegeDefine.READ + "|" + ScmPrivilegeDefine.UPDATE + "|"
                    + ScmPrivilegeDefine.DELETE,

            ScmPrivilegeDefine.CREATE + "|" + ScmPrivilegeDefine.UPDATE + "|"
                    + ScmPrivilegeDefine.DELETE,

            ScmPrivilegeDefine.CREATE + "|" + ScmPrivilegeDefine.READ + "|"
                    + ScmPrivilegeDefine.UPDATE + "|"
                    + ScmPrivilegeDefine.DELETE,

            ScmPrivilegeDefine.ALL };
    private String[] dirpaths = { "/1735_A/1735_B", "/1735_B/1735_C",
            "/1735_C/1735_D", "/1735_D/1735_E",

            "/1735_E/1735_E/1735_G", "/1735_E/1735_E/1735_H",

            "/1736_E/1736_E/1736_G",
            "/1736_E/1736_F/1736_A/1736_B/1736_C/1736_D",

            "/1736_F", "/1736_H", "/1736_H/1736_A",

            "/H_1737/I_1737/A_1737",
            "/A_1737/TEXT_1737/MV_1737/WORD_B/PICTURE_C/EngLish_D",

            "/LANGUAGE_1737_1", "/TEXT_1737_BACKUP", "/1737_I",

            "/OneSevenThreeFive_Four", "/SevenThreeSevenOne_Three",
            "/SixThreeSevenOne_Two",

            "/Document_Over" };

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession( site );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIncludeDirrsInHead() throws ScmException {
        String path = dirpaths[ 0 ] + "/a_1735/b_1735";
        ScmDirectory actDir = null;
        try {
            actDir = createDir( wsA, path );
            // have priority to read
            ScmDirectory readDir = ScmFactory.Directory.getInstance( wsUser,
                    path );
            Assert.assertEquals( readDir.getPath(), path + "/" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        try {
            // doesn't have prority to delete
            ScmFactory.Directory.deleteInstance( wsUser, path );
            Assert.fail(
                    "the user does not have priority to delete dir = " + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( actDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIncludeDirrsInMiddle() throws ScmException {
        String path = dirpaths[ 9 ] + "/1736_" + UUID.randomUUID();
        ScmDirectory actDir = null;
        try {
            actDir = ScmFactory.Directory.createInstance( wsA, path );
            // have priority to delete
            ScmFactory.Directory.deleteInstance( wsUser, path );

            // check delete
            ScmFactory.Directory.getInstance( wsA, path );
            Assert.fail(
                    "the user have delete priority but delete dir fail,dir = "
                            + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( actDir != null ) {
                try {
                    ScmFactory.Directory.deleteInstance( wsA, path );
                } catch ( ScmException e ) {
                    System.out.println( "delete inexist dir" );
                }
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testLeftOfCenter() throws ScmException {
        String path = dirpaths[ 6 ] + "/1736_" + UUID.randomUUID();
        try {
            // have priority to CREATE
            ScmDirectory createDir = ScmFactory.Directory
                    .createInstance( wsUser, path );

            // have priority to READ
            ScmDirectory readDir = ScmFactory.Directory.getInstance( wsUser,
                    path );
            Assert.assertEquals( readDir.getName(), createDir.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        try {
            // doesn't have prority to delete
            ScmFactory.Directory.deleteInstance( wsUser, path );
            Assert.fail(
                    "the user does not have priority to delete dir = " + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            ScmFactory.Directory.deleteInstance( wsA, path );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRightOfCenter() throws ScmException {
        String path = dirpaths[ 10 ] + "/1739_" + UUID.randomUUID();
        try {
            // have the prority of ALL
            // create
            ScmFactory.Directory.createInstance( wsUser, path );

            // read
            ScmDirectory readDir = ScmFactory.Directory.getInstance( wsUser,
                    path );

            // update
            String newname = "1738_update";
            String newpath = dirpaths[ 10 ] + "/" + newname;
            readDir.rename( newname );

            // read again
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsUser,
                    newpath );
            Assert.assertEquals( dir.getName(), newname );

            // delete
            ScmFactory.Directory.deleteInstance( wsUser, newpath );

            // check delete
            ScmFactory.Directory.getInstance( wsA, path );
            Assert.fail(
                    "the user have delete priority but delete dir fail,dir = "
                            + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIncludeDirrsInTail() throws ScmException {
        String path = dirpaths[ 19 ] + "/1738_" + UUID.randomUUID();
        ScmDirectory actDir = null;
        try {
            // have prority to CREATE
            actDir = ScmFactory.Directory.createInstance( wsUser, path );
            // have prority DELETE
            ScmFactory.Directory.deleteInstance( wsUser, path );
            actDir = null;
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        try {
            // have prority to CREATE
            actDir = ScmFactory.Directory.createInstance( wsUser, path );

            // does not have priority to READ
            ScmFactory.Directory.getInstance( wsUser, path );
            Assert.fail(
                    "the user does not have priority to create dir= " + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( actDir != null ) {
                ScmFactory.Directory.deleteInstance( wsUser, path );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testUnrelated() throws ScmException {
        String path = "/1735_AuthWs_Dir1737";
        ScmDirectory actDir = null;
        try {
            actDir = ScmFactory.Directory.createInstance( wsA, path );
            ScmFactory.Directory.getInstance( wsUser, path );
            Assert.fail(
                    "the user does not have priority to read dir= " + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( actDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            for ( int i = 0; i < dirrsList.size(); i++ ) {
                ScmFactory.Role.revokePrivilege( sessionA, role,
                        dirrsList.get( i ),
                        privileges[ i % privileges.length ] );
            }
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
            for ( String dirpath : dirpaths ) {
                deleteDir( wsA, dirpath );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role, String privileges )
            throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( session, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );
    }

    private ScmDirectory createDir( ScmWorkspace ws, String dirPath )
            throws ScmException {
        List< String > pathList = getSubPaths( dirPath );
        for ( String path : pathList ) {
            try {
                ScmFactory.Directory.createInstance( ws, path );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
        return ScmFactory.Directory.getInstance( ws,
                pathList.get( pathList.size() - 1 ) );
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

    private void cleanEnv() {
        try {
            ScmFactory.Role.deleteRole( sessionA, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.User.deleteUser( sessionA, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepare() throws Exception {
        try {
            user = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );
            Arrays.sort( dirpaths );
            for ( int i = 0; i < dirpaths.length; i++ ) {
                deleteDir( wsA, dirpaths[ i ] );
                createDir( wsA, dirpaths[ i ] );
                ScmResource dirrs = ScmResourceFactory.createDirectoryResource(
                        wsp.getName(), dirpaths[ i ] );
                dirrsList.add( dirrs );
                System.out.println(
                        "dir" + i + " = " + dirpaths[ i ] + ",privilege = "
                                + privileges[ i % privileges.length ] );
                grantPriAndAttachRole( sessionA, dirrs, user, role,
                        privileges[ i % privileges.length ] );
            }

            ScmAuthUtils.checkPriority( site, username, passwd, role,
                    wsp.getName() );

            sessionUser = TestScmTools.createSession( site, username, passwd );
            wsUser = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionUser );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
