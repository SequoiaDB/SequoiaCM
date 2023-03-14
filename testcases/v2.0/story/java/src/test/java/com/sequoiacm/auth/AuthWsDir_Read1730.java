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
import com.sequoiacm.client.core.ScmCursor;
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
import com.sequoiacm.client.element.ScmFileBasicInfo;
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
 * @Description: SCM-1730 ::
 *               有工作区的READ权限和目录的CREATE/UPDATE/DELETE权限，对表格中的各个接口进行覆盖测试
 * @Date:2018年6月7日
 * @version:1.0
 */
public class AuthWsDir_Read1730 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private ScmSession sessionCRUD;
    private ScmWorkspace wsCRUD;
    private String username = "AuthDir_Read1726";
    private String rolename = "Role_1726_R";
    private String passwd = "1726";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs1;
    private ScmResource rs2;
    private String basepath = "/AuthDir_Read1726";
    private String path = basepath + "/1726_A/1726_B/1726_C";
    private String author = "AuthDir_Read1726";
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
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testGetDirByPath() {
        String dirpath = path;
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsCRUD,
                    dirpath );
            Assert.assertEquals( dir.getPath(), dirpath + "/" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testGetFileMetaUnderDir() throws ScmException {
        ScmId fileId = null;
        String fileName = author + "_" + UUID.randomUUID();
        String dirpath = path;
        try {
            // get dir
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsCRUD,
                    dirpath );

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance( wsCRUD );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( dir );
            file.setContent( filePath );
            fileId = file.save();

            // get file
            ScmDirectory actdir = ScmFactory.Directory.getInstance( wsCRUD,
                    dirpath );
            ScmFile actfile = actdir.getSubfile( fileName );
            Assert.assertEquals( actfile.getDirectory().getPath(),
                    dirpath + "/" );
            Assert.assertEquals( actfile.getFileName(), fileName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsCRUD, fileId, true );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testReadFileById() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        String dirpath = path;
        try {
            // get dir
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsCRUD,
                    dirpath );

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance( wsCRUD );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( dir );
            file.setContent( filePath );
            fileId = file.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsCRUD, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            actfile.getContent( downloadPath );
            Assert.assertEquals( actfile.getFileName(), fileName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsCRUD, fileId, true );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testReadFileByPath() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        String dirpath = path;
        try {
            // get dir
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsCRUD,
                    dirpath );

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance( wsCRUD );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( dir );
            file.setContent( filePath );
            fileId = file.save();

            ScmFile actfile = ScmFactory.File.getInstanceByPath( wsCRUD,
                    dirpath + "/" + fileName );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            actfile.getContent( downloadPath );
            Assert.assertEquals( actfile.getFileName(), fileName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsCRUD, fileId, true );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testReadFileByIdVersion() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        String dirpath = path;
        try {
            // get dir
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsA, dirpath );

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( dir );
            file.setContent( filePath );
            fileId = file.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsCRUD, fileId, 1,
                    0 );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            actfile.getContent( downloadPath );
            Assert.assertEquals( actfile.getFileName(), fileName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsCRUD, fileId, true );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testReadFileByPathVersion() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        String dirpath = path;
        try {
            // get dir
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsA, dirpath );

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( dir );
            file.setContent( filePath );
            fileId = file.save();

            ScmFile actfile = ScmFactory.File.getInstanceByPath( wsCRUD,
                    dirpath + "/" + fileName, 1, 0 );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            actfile.getContent( downloadPath );
            Assert.assertEquals( actfile.getFileName(), fileName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsCRUD, fileId, true );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testlistFile() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        String dirpath = path;
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            // get dir
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsA, dirpath );

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( dir );
            file.setContent( filePath );
            fileId = file.save();

            ScmDirectory actdir = ScmFactory.Directory.getInstance( wsCRUD,
                    dirpath );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();

            cursor = actdir.listFiles( cond );
            Assert.assertEquals( cursor.getNext().getFileName(), fileName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsCRUD, fileId, true );
            }
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, rs1,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.revokePrivilege( sessionA, role, rs2,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, rs2,
                    ScmPrivilegeType.DELETE );
            ScmFactory.Role.revokePrivilege( sessionA, role, rs2,
                    ScmPrivilegeType.UPDATE );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
            deleteDir( wsA, path );
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
        List< String > pathList = new ArrayList<>();
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

            deleteDir( wsA, path );
            createDir( wsA, path );
            rs1 = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            rs2 = ScmResourceFactory.createDirectoryResource( wsp.getName(),
                    path );
            grantPriAndAttachRole( sessionA, rs1, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, rs2, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, rs2, user, role,
                    ScmPrivilegeType.DELETE );
            grantPriAndAttachRole( sessionA, rs2, user, role,
                    ScmPrivilegeType.UPDATE );

            ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );

            sessionCRUD = ScmSessionUtils.createSession( site, username, passwd );
            wsCRUD = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionCRUD );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
