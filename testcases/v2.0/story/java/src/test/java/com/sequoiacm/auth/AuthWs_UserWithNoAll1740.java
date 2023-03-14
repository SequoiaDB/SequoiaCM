package com.sequoiacm.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.client.element.privilege.ScmPrivilegeDefine;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description: SCM-1740 :: 对角色授权，当前用户对资源无ALL的权限
 * @author fanyu
 * @Date:2018年6月8日
 * @version:1.0
 */
public class AuthWs_UserWithNoAll1740 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private String[] usernameArr = { "AuthWs_UserWithNoAll1740_0",
            "AuthWs_UserWithNoAll1740_1", "AuthWs_UserWithNoAll1740_2" };
    private String[] rolenameArr = { "1740_0", "1740_1", "1740_2" };
    private String[] privileges = { ScmPrivilegeDefine.READ,
            ScmPrivilegeDefine.CREATE + "|" + ScmPrivilegeDefine.READ + "|"
                    + ScmPrivilegeDefine.DELETE + "|"
                    + ScmPrivilegeDefine.UPDATE,
            null };
    private String passwd = "1740";
    private List< ScmUser > userList = new ArrayList< ScmUser >();
    private List< ScmRole > roleList = new ArrayList< ScmRole >();
    private ScmResource rs;
    private WsWrapper wsp;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            sessionA = ScmSessionUtils.createSession( site );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSinglePriv() throws ScmException {
        ScmSession session = null;
        String username = "1740_R";
        String rolename = "ROlE_1740_R";
        ScmUser user = null;
        ScmRole role = null;
        try {
            session = ScmSessionUtils.createSession( site, usernameArr[ 0 ],
                    passwd );
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( session, rolename, null );
            ScmResource rs = ScmResourceFactory
                    .createWorkspaceResource( wsp.getName() );
            grantPriAndAttachRole( session, rs, user, role,
                    ScmPrivilegeDefine.ALL );
            Assert.fail( "exp fail but act success,user = " + user.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_FORBIDDEN ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( role != null ) {
                ScmFactory.Role.deleteRole( sessionA, role );
            }
            if ( user != null ) {
                ScmFactory.Role.deleteRole( sessionA, rolename );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCRUDPriv() throws ScmException {
        ScmSession session = null;
        String username = "1740_CRUD";
        String rolename = "ROlE_1740_CRUD";
        ScmUser user = null;
        ScmRole role = null;
        try {
            session = ScmSessionUtils.createSession( site, usernameArr[ 1 ],
                    passwd );
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( session, rolename, null );
            ScmResource rs = ScmResourceFactory
                    .createWorkspaceResource( wsp.getName() );
            grantPriAndAttachRole( session, rs, user, role,
                    ScmPrivilegeDefine.CREATE + "|" + ScmPrivilegeDefine.READ );
            Assert.fail( "exp fail but act success,user = " + user.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_FORBIDDEN ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( role != null ) {
                ScmFactory.Role.deleteRole( sessionA, role );
            }
            if ( user != null ) {
                ScmFactory.Role.deleteRole( sessionA, rolename );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNullPriv() throws ScmException {
        ScmSession session = null;
        String username = "1740_NULL";
        String rolename = "ROlE_1740_NULL";
        ScmUser user = null;
        ScmRole role = null;
        try {
            session = ScmSessionUtils.createSession( site, usernameArr[ 2 ],
                    passwd );
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( session, rolename, null );
            ScmResource rs = ScmResourceFactory
                    .createWorkspaceResource( wsp.getName() );
            grantPriAndAttachRole( session, rs, user, role,
                    ScmPrivilegeDefine.CREATE + "|" + ScmPrivilegeDefine.READ );
            Assert.fail( "exp fail but act success,user = " + user.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_FORBIDDEN ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( role != null ) {
                ScmFactory.Role.deleteRole( sessionA, role );
            }
            if ( user != null ) {
                ScmFactory.Role.deleteRole( sessionA, rolename );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            for ( int i = 0; i < usernameArr.length; i++ )
                try {
                    if ( i < usernameArr.length - 1 ) {
                        ScmFactory.Role.revokePrivilege( sessionA,
                                roleList.get( i ), rs, privileges[ i ] );
                    }
                    ScmFactory.Role.deleteRole( sessionA, roleList.get( i ) );
                    ScmFactory.User.deleteUser( sessionA, userList.get( i ) );
                } catch ( Exception e ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
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
        ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( sessionA, user, modifier );
    }

    private void cleanEnv() {
        for ( String rolename : rolenameArr ) {
            try {
                ScmFactory.Role.deleteRole( sessionA, rolename );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
        for ( String username : usernameArr ) {
            try {
                ScmFactory.User.deleteUser( sessionA, username );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private void prepare() throws Exception {
        rs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
        for ( int i = 0; i < usernameArr.length; i++ ) {
            try {
                ScmUser user = ScmFactory.User.createUser( sessionA,
                        usernameArr[ i ], ScmUserPasswordType.LOCAL, passwd );
                ScmRole role = ScmFactory.Role.createRole( sessionA,
                        rolenameArr[ i ], null );
                if ( i < usernameArr.length - 1 ) {
                    grantPriAndAttachRole( sessionA, rs, user, role,
                            privileges[ i ] );
                    ScmAuthUtils.checkPriority( site, user.getUsername(),
                            passwd, role, wsp );
                } else {
                    ScmUserModifier modifier = new ScmUserModifier();
                    modifier.addRole( role );
                    ScmFactory.User.alterUser( sessionA, user, modifier );
                }
                userList.add( user );
                roleList.add( role );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }
}
