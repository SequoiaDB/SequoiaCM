package com.sequoiacm.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @author fanyu
 * @Description: SCM-1744 :: 用户拥有多个角色，用继承的权限操作业务
 * @Date:2018年6月8日
 * @version:1.0
 */
public class AuthWs_UserHasDiffRolePriv1744 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String username = "AuthWs_UserHasDiffRolePriv1744";
    private String[] rolenameArr = { "1744_0", "1744_1", "1744_2" };
    private ScmPrivilegeType[] privileges = { ScmPrivilegeType.READ,
            ScmPrivilegeType.ALL, ScmPrivilegeType.ALL };
    private String passwd = "1744";
    private ScmUser user = null;
    private List< ScmRole > roleList = new ArrayList< ScmRole >();
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
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
        }
    }

    @Test(groups = { GroupTags.base })
    private void testWs() throws ScmException {
        ScmSession session = null;
        String fileName = "AuthWs_UserHasDiffRolePriv1744_0";
        ScmId fileId = null;
        try {
            session = ScmSessionUtils.createSession( site, username, passwd );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            fileId = ScmFileUtils.create( ws, fileName, filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { GroupTags.base })
    private void testDir() throws ScmException {
        ScmSession session = null;
        String dirpath = "/1744_1/1174_A";
        ScmDirectory expdir = null;
        ScmDirectory actdir = null;
        try {
            session = ScmSessionUtils.createSession( site, username, passwd );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            expdir = ScmFactory.Directory.createInstance( ws, "/1744_1" );
            expdir.createSubdirectory( "1174_A" );

            actdir = ScmFactory.Directory.getInstance( ws, dirpath );
            Assert.assertEquals( expdir.getPath(),
                    actdir.getParentDirectory().getPath() );

        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( actdir != null ) {
                actdir.delete();
            }
            if ( expdir != null ) {
                expdir.delete();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmResource rs = ScmResourceFactory
                    .createWorkspaceResource( wsp.getName() );
            for ( int i = 0; i < rolenameArr.length; i++ ) {
                try {
                    if ( i == rolenameArr.length - 1 ) {
                        rs = ScmResourceFactory
                                .createDirectoryResource( wsp.getName(), "/" );
                    }
                    ScmFactory.Role.revokePrivilege( sessionA,
                            roleList.get( i ), rs, privileges[ i ] );
                    ScmFactory.Role.deleteRole( sessionA, roleList.get( i ) );
                } catch ( Exception e ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
            try {
                ScmFactory.User.deleteUser( sessionA, user );
                TestTools.LocalFile.removeFile( localPath );
            } catch ( ScmException e ) {
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
            ScmUser user, ScmRole role, ScmPrivilegeType privileges )
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
        user = ScmFactory.User.createUser( sessionA, username,
                ScmUserPasswordType.LOCAL, passwd );
        ScmResource rs = ScmResourceFactory
                .createWorkspaceResource( wsp.getName() );
        for ( int i = 0; i < rolenameArr.length; i++ ) {
            try {
                if ( i == rolenameArr.length - 1 ) {
                    rs = ScmResourceFactory
                            .createDirectoryResource( wsp.getName(), "/" );
                }
                ScmRole role = ScmFactory.Role.createRole( sessionA,
                        rolenameArr[ i ], null );
                grantPriAndAttachRole( sessionA, rs, user, role,
                        privileges[ i ] );
                ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );
                roleList.add( role );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }
}
