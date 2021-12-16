package com.sequoiacm.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1730 ::有工作区DELETE|UPDATE|READ的权限和目录的CREATE权限，对表格中的各个接口进行覆盖测试
 * @author fanyu
 * @Date:2018年6月12日
 * @version:1.0
 */
public class AuthWsDir_CreateDelete1730 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmSession sessionCRUD;
    private ScmWorkspace wsA;
    private ScmWorkspace wsCRUD;
    private String username = "AuthWsDir_CreateDelete1730";
    private String rolename = "Role_1730_CRUD";
    private String passwd = "1730";
    private ScmUser user;
    private ScmRole role;
    private ScmResource wsrs;
    private ScmResource dirrs;
    private String basepath = "/AuthWsDir_CreateDelete1730";
    private String path1 = basepath + "/1730_A/1730_B/1730_C/1730_D";
    private String path2 = basepath + "/1730_E/1730_F/1730_G";
    private String author = "AuthWsDir_CreateDelete1730";
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
    private void test() throws ScmException {
        testMoveDir();
        testReNameDir();
    }

    private void testMoveDir() throws ScmException {
        String srcpath = path2;
        String dstpath = path1;
        String newpath = path1 + "/1730_G";
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmDirectory srcDir = ScmFactory.Directory.getInstance( wsCRUD,
                    srcpath );
            ScmDirectory dstDir = ScmFactory.Directory.getInstance( wsCRUD,
                    dstpath );
            srcDir.move( dstDir );
            ScmDirectory actdir = ScmFactory.Directory.getInstance( wsCRUD,
                    newpath );
            Assert.assertEquals( actdir.getPath(), newpath + "/" );

            // check dir
            ScmFile expfile = ScmFactory.File.createInstance( wsCRUD );
            expfile.setFileName( fileName );
            expfile.setAuthor( author );
            expfile.setDirectory( actdir );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsCRUD, fileId );
            Assert.assertEquals( actfile.getFileName(), expfile.getFileName() );
            Assert.assertEquals( actfile.getDirectory().getPath(),
                    expfile.getDirectory().getPath() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    private void testReNameDir() throws ScmException {
        String path = path1;
        String newdirName = "1730_F";
        String newpath = basepath + "/1730_A/1730_B/1730_C/1730_F";
        String fileName = author + "_" + UUID.randomUUID();
        String subdirname = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsCRUD, path );
            dir.rename( newdirName );

            // check dir
            ScmDirectory actDir = ScmFactory.Directory.getInstance( wsCRUD,
                    newpath );
            Assert.assertEquals( actDir.getPath(), newpath + "/" );
            ScmDirectory subdir = actDir.createSubdirectory( subdirname );
            subdir.delete();

            ScmFile expfile = ScmFactory.File.createInstance( wsCRUD );
            expfile.setFileName( fileName );
            expfile.setAuthor( author );
            expfile.setDirectory( actDir );
            fileId = expfile.save();
            ScmFile actfile = ScmFactory.File.getInstance( wsCRUD, fileId );
            Assert.assertEquals( actfile.getFileName(), expfile.getFileName() );
            Assert.assertEquals( actfile.getDirectory().getPath(),
                    expfile.getDirectory().getPath() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsCRUD, fileId, true );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.DELETE );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.DELETE );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.UPDATE );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
            deleteDir( wsA, basepath + "/1730_A/1730_B/1730_C/1730_F/1730_G" );
            deleteDir( wsA, path1 );
            deleteDir( wsA, basepath + "/1730_E/1730_F" );
            TestTools.LocalFile.removeFile( localPath );
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
            ScmUser user, ScmRole role, ScmPrivilegeType privileges ) {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
            modifier.addRole( role );
            ScmFactory.User.alterUser( sessionA, user, modifier );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
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

    private void cleanEnv() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        ScmFileUtils.cleanFile( wsp, cond );
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

            deleteDir( wsA, basepath + "/1730_A/1730_B/1730_C/1730_G" );
            deleteDir( wsA, path1 );
            deleteDir( wsA, basepath + "/1730_E/1730_F" );
            createDir( wsA, path1 );
            createDir( wsA, path2 );

            wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            dirrs = ScmResourceFactory.createDirectoryResource( wsp.getName(),
                    basepath + "/1730_A/1730_B/1730_C/" );

            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.DELETE );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.UPDATE );

            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.CREATE );
            ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );

            sessionCRUD = TestScmTools.createSession( site, username, passwd );
            wsCRUD = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionCRUD );

        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
