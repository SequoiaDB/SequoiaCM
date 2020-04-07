package com.sequoiacm.net.auth.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;

/**
 * @author fanyu
 * @Description: SCM-1728 :: 有目录资源的ALL的权限，对表格中各个接口进行覆盖测试
 * @Date:2018年6月13日
 * @version:1.0
 */
public class AuthDir_All1728 extends TestScmBase {
    private SiteWrapper rootsite;
    private SiteWrapper branchsite;
    private ScmSession sessionA;
    private ScmSession sessionALL;
    private ScmWorkspace wsA;
    private ScmWorkspace wsALL;
    private String username = "AuthDir_All1728";
    private String rolename = "Role_1728_ALL";
    private String passwd = "1728";
    private ScmUser user;
    private ScmRole role;
    private String basepath = "/AuthDir_All1728";
    private String path = basepath + "/1728_A/1728_B/1728_C";
    private String path1 = "/1728_E/1728_F/1728_G";
    private ScmResource rs;
    private ScmResource rs1;
    private String author = "AuthDir_Create1728";
    private WsWrapper wsp;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator +
                    TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize +
                    ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            wsp = ScmInfo.getWs();
            List< SiteWrapper > sites = ScmNetUtils.getSortSites( wsp );
            rootsite = sites.get( 1 );
            branchsite = sites.get( 0 );

            sessionA = TestScmTools.createSession( rootsite );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    // C+R
    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateDir() {
        String subpath = path + "/1726_D";
        try {
            ScmDirectory expdir = ScmFactory.Directory
                    .createInstance( wsALL, subpath );
            ScmDirectory actdir = ScmFactory.Directory
                    .getInstance( wsALL, subpath );
            Assert.assertEquals( expdir.getPath(), actdir.getPath() );
            actdir.delete();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateFileInDir() throws ScmException {
        ScmId fileId = null;
        String fileName = author + "_" + UUID.randomUUID();
        String subpath = path + "/1726_E";
        try {
            // create dir
            ScmDirectory actdir = ScmFactory.Directory
                    .createInstance( wsALL, subpath );
            ScmFile file = ScmFactory.File.createInstance( wsALL );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( actdir );
            fileId = file.save();

            // check
            ScmDirectory dir = ScmFactory.Directory
                    .getInstance( wsALL, subpath );
            ScmFile actfile = dir.getSubfile( fileName );
            Assert.assertEquals( actfile.getDirectory().getPath(),
                    subpath + "/" );
            ScmFactory.File.deleteInstance( wsA, fileId, true );
            dir.delete();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    // D
    @Test(groups = { "twoSite", "fourSite" })
    private void testDeleteFile() throws ScmException {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try {
            dir = ScmFactory.Directory.getInstance( wsA, dirPath );
            ScmFile expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            expfile.setContent( filePath );
            fileId = expfile.save();

            ScmFactory.File.deleteInstance( wsALL, fileId, true );
            fileId = null;
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testDeleteScmDir() throws ScmException {
        String dirpath = path + "/" + author + "_" + UUID.randomUUID();
        ScmDirectory expDir = null;
        try {
            expDir = ScmFactory.Directory.createInstance( wsALL, dirpath );
            ScmFactory.Directory.deleteInstance( wsALL, dirpath );
            expDir = null;
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expDir != null ) {
                ScmFactory.Directory.deleteInstance( wsALL, dirpath );
            }
        }
    }

    // U+R
    @Test(groups = { "twoSite", "fourSite" })
    private void testUpdateFile() throws ScmException {
        String fileName = author + "_" + UUID.randomUUID();
        String newfileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirpath = path;
        try {
            // getdir
            dir = ScmFactory.Directory.getInstance( wsA, dirpath );
            ScmFile expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsALL, fileId );
            actfile.setFileName( newfileName );

            ScmFile expFile1 = ScmFactory.File.getInstance( wsALL, fileId );
            Assert.assertEquals( actfile.getFileName(),
                    expFile1.getFileName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynCaheFile() throws ScmException {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try { // get dir
            dir = ScmFactory.Directory.getInstance( wsA, dirPath );
            ScmFile expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            fileId = expfile.save();

            ScmFactory.File.asyncCache( wsALL, fileId );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynCaheFileByVersion() throws ScmException {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try { // get dir
            dir = ScmFactory.Directory.getInstance( wsA, dirPath );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), sessionA );
            ScmFile expfile = ScmFactory.File.createInstance( ws );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            expfile.setContent( filePath );
            fileId = expfile.save();

            ScmFactory.File.asyncCache( wsALL, fileId, 1, 0 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsALL, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynTransfer() throws ScmException {
        ScmSession session = null;
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try {
            session = TestScmTools.createSession( branchsite );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session );
            // get dir
            dir = ScmFactory.Directory.getInstance( ws, dirPath );
            ScmFile expfile = ScmFactory.File.createInstance( ws );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            expfile.setContent( filePath );
            fileId = expfile.save();

            ScmFactory.File.asyncTransfer( wsALL, fileId );
            ;
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsALL, fileId, true );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynTransferByVersion() throws ScmException {
        ScmSession session = null;
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try {
            session = TestScmTools.createSession( branchsite );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session ); // get
            // dir
            dir = ScmFactory.Directory.getInstance( ws, dirPath );
            ScmFile expfile = ScmFactory.File.createInstance( ws );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            expfile.setContent( filePath );
            fileId = expfile.save();

            ScmFactory.File.asyncTransfer( wsALL, fileId, 1, 0 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsALL, fileId, true );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    // none
    @Test(groups = { "twoSite", "fourSite" })
    private void testListDir() {
        ScmCursor< ScmDirectory > cursor = null;
        try {
            cursor = ScmFactory.Directory
                    .listInstance( wsALL, new BasicBSONObject() );
            Assert.assertNotNull( cursor );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListSche() {
        ScmCursor< ScmScheduleBasicInfo > cursor = null;
        try {
            cursor = ScmSystem.Schedule
                    .list( sessionALL, new BasicBSONObject() );
            Assert.assertNotNull( cursor );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testMoveDir() throws ScmException {
        String srcpath = path1 + "/1728_H_M";
        String dstpath = path;
        String newpath = path + "/1728_H_M";
        String fileName = author + "_" + UUID.randomUUID();
        String subdirname = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmDirectory srcDir = ScmFactory.Directory
                    .createInstance( wsALL, srcpath );
            ScmDirectory dstDir = ScmFactory.Directory
                    .getInstance( wsALL, dstpath );
            srcDir.move( dstDir );
            ScmDirectory actdir = ScmFactory.Directory
                    .getInstance( wsALL, newpath );
            Assert.assertEquals( actdir.getPath(), newpath + "/" );

            // check dir
            ScmDirectory subdir = actdir.createSubdirectory( subdirname );
            subdir.delete();

            ScmFile expfile = ScmFactory.File.createInstance( wsALL );
            expfile.setFileName( fileName );
            expfile.setAuthor( author );
            expfile.setDirectory( actdir );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsALL, fileId );
            Assert.assertEquals( actfile.getFileName(), expfile.getFileName() );
            Assert.assertEquals( actfile.getDirectory().getPath(),
                    expfile.getDirectory().getPath() );
            ScmFactory.File.deleteInstance( wsALL, fileId, true );
            // delete
            actdir.delete();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testReNameDir() throws ScmException {
        String path = path1 + "/1728_H";
        String newdirName = "1728_I";
        String newpath = path1 + "/1728_I";
        String fileName = author + "_" + UUID.randomUUID();
        String subdirname = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmFactory.Directory.createInstance( wsALL, path );
            ScmDirectory dir1 = ScmFactory.Directory.getInstance( wsALL, path );
            dir1.rename( newdirName );

            // check dir
            ScmDirectory actDir = ScmFactory.Directory
                    .getInstance( wsALL, newpath );
            Assert.assertEquals( actDir.getPath(), newpath + "/" );
            ScmDirectory subdir = actDir.createSubdirectory( subdirname );
            subdir.delete();

            ScmFile expfile = ScmFactory.File.createInstance( wsALL );
            expfile.setFileName( fileName );
            expfile.setAuthor( author );
            expfile.setDirectory( actDir );
            fileId = expfile.save();
            ScmFile actfile = ScmFactory.File.getInstance( wsALL, fileId );
            Assert.assertEquals( actfile.getFileName(), expfile.getFileName() );
            Assert.assertEquals( actfile.getDirectory().getPath(),
                    expfile.getDirectory().getPath() );
            ScmFactory.File.deleteInstance( wsALL, fileId, true );

            // delete
            actDir.delete();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testMoveFile() throws ScmException {
        String oldpath = path;
        String newpath = path1;
        String fileName = author + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmDirectory dir1 = ScmFactory.Directory
                    .getInstance( wsALL, oldpath );

            ScmFile expfile = ScmFactory.File.createInstance( wsALL );
            expfile.setFileName( fileName );
            expfile.setAuthor( author );
            expfile.setDirectory( dir1 );
            fileId = expfile.save();

            // move file
            ScmFile file = ScmFactory.File
                    .getInstanceByPath( wsALL, oldpath + "/" + fileName );
            ScmDirectory newDir = ScmFactory.Directory
                    .getInstance( wsALL, newpath );
            file.setDirectory( newDir );

            // check
            ScmFile actfile = ScmFactory.File
                    .getInstanceByPath( wsALL, newpath + "/" + fileName );
            Assert.assertEquals( actfile.getFileName(), expfile.getFileName() );
            Assert.assertEquals( actfile.getDirectory().getPath(),
                    newpath + "/" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsALL, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testRenameFile() throws ScmException {
        String path = path1;
        String fileName = author + UUID.randomUUID();
        String newFileName = fileName + "_0";
        ScmId fileId = null;
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsALL, path );
            ScmFile file = ScmFactory.File.createInstance( wsALL );
            file.setFileName( fileName );
            file.setAuthor( author );
            file.setDirectory( dir );
            fileId = file.save();

            // rename file
            ScmFile file1 = ScmFactory.File
                    .getInstanceByPath( wsALL, path + "/" + fileName );
            file1.setFileName( newFileName );

            // check
            ScmFile actfile = ScmFactory.File
                    .getInstanceByPath( wsALL, path + "/" + newFileName );
            Assert.assertEquals( actfile.getFileName(), newFileName );
            Assert.assertEquals( actfile.getDirectory().getPath(), path + "/" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsALL, fileId, true );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.ALL );
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.ALL );
            ScmFactory.User.deleteUser( sessionA, user );
            deleteDir( wsA, path );
            deleteDir( wsA, path1 );
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
            ScmUser user, ScmRole role,
            ScmPrivilegeType privileges ) {
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
        return ScmFactory.Directory
                .getInstance( ws, pathList.get( pathList.size() - 1 ) );
    }

    private void deleteDir( ScmWorkspace ws, String dirPath ) {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND &&
                        e.getError() != ScmError.DIR_NOT_EMPTY ) {
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
        deleteDir( wsA, path );
        deleteDir( wsA, path1 );
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
            user = ScmFactory.User
                    .createUser( sessionA, username, ScmUserPasswordType.LOCAL,
                            passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );

            rs = ScmResourceFactory
                    .createDirectoryResource( wsp.getName(), path );
            rs1 = ScmResourceFactory
                    .createDirectoryResource( wsp.getName(), path1 );

            createDir( wsA, path );
            createDir( wsA, path1 );
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.ALL );
            grantPriAndAttachRole( sessionA, rs1, user, role,
                    ScmPrivilegeType.ALL );

            ScmAuthUtils.checkPriority( rootsite, username, passwd, role, wsp );

            sessionALL = TestScmTools
                    .createSession( branchsite, username, passwd );
            wsALL = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), sessionALL );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
