package com.sequoiacm.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BreakpointInputStream;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @author fanyu
 * @Description:1723::有工作区READ权限，对表格中的各个接口进行覆盖测试
 * @Date:2018年6月6日
 * @version:1.0
 */
public class AuthWs_Read1723 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmSession sessionR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsR;
    private String username = "AuthWs_Read1723";
    private String rolename = "1723_R";
    private String passwd = "1723";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs;
    private String author = "AuthWs_Read1723";
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

            site = ScmInfo.getRootSite();
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

    @Test(groups = { "twoSite", "fourSite" })
    private void testListBatch() {
        BSONObject bson = new BasicBSONObject();
        ScmCursor< ScmBatchInfo > cursor = null;
        try {
            cursor = ScmFactory.Batch.listInstance( wsR, bson );
            Assert.assertEquals( cursor != null, true );
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
    private void testGetBatch() throws ScmException {
        String batchName = "AuthWs_Read1723";
        ScmId batchId = null;
        try {
            ScmBatch expBatch = ScmFactory.Batch.createInstance( wsA );
            expBatch.setName( batchName );
            batchId = expBatch.save();
            ScmBatch actBatch = ScmFactory.Batch.getInstance( wsR,
                    expBatch.getId() );
            Assert.assertEquals( actBatch.getName(), expBatch.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( batchId != null ) {
                ScmFactory.Batch.deleteInstance( wsA, batchId );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListBreakpointFile() {
        BSONObject cond = new BasicBSONObject();
        ScmCursor< ScmBreakpointFile > cursor = null;
        try {
            cursor = ScmFactory.BreakpointFile.listInstance( wsR, cond );
            Assert.assertEquals( cursor != null, true );
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
    private void testListBreakpointFileByWs() {
        ScmCursor< ScmBreakpointFile > cursor = null;
        try {
            cursor = ScmFactory.BreakpointFile.listInstance( wsR );
            Assert.assertEquals( cursor != null, true );
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
    private void testReadBreakpointFileByName()
            throws IOException, ScmException {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile.createInstance( wsA,
                    fileName );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ) );
            expBreakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();

            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .getInstance( wsR, fileName );

            Assert.assertEquals( breakpointFile.getFileName(),
                    expBreakpointFile.getFileName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expBreakpointFile != null ) {
                ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadDir() throws ScmException {
        String path = "/AuthWs_Read1723" + "_" + UUID.randomUUID();
        ScmDirectory expDir = null;
        try {
            expDir = ScmFactory.Directory.createInstance( wsA, path );
            ScmDirectory actDir = ScmFactory.Directory.getInstance( wsR, path );
            Assert.assertEquals( actDir.getPath(), expDir.getPath() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testGetFileMetaUnderDir() throws ScmException {
        ScmId fileId = null;
        String fileName = author + "_" + UUID.randomUUID();
        String dirpath = "/AuthWs_Read1723" + "_" + UUID.randomUUID();
        ScmDirectory expDir = null;
        try {
            // create dir
            expDir = ScmFactory.Directory.createInstance( wsA, dirpath );

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( expDir );
            file.setContent( filePath );
            fileId = file.save();

            // get file
            ScmDirectory actdir = ScmFactory.Directory.getInstance( wsR,
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
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
            if ( expDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, dirpath );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testlistFile() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        String dirpath = "/" + author + "_" + UUID.randomUUID();
        ScmCursor< ScmFileBasicInfo > cursor = null;
        ScmDirectory expDir = null;
        try {
            // create dir
            expDir = ScmFactory.Directory.createInstance( wsA, dirpath );

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setAuthor( author );
            file.setFileName( fileName );
            file.setDirectory( expDir );
            file.setContent( filePath );
            fileId = file.save();

            ScmDirectory actdir = ScmFactory.Directory.getInstance( wsR,
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
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
            if ( cursor != null ) {
                cursor.close();
            }
            if ( expDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, dirpath );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadBreakpointFileBySize()
            throws IOException, ScmException {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile.createInstance( wsA,
                    fileName );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ) );
            expBreakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();

            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .getInstance( wsR, fileName, 0 );

            Assert.assertEquals( breakpointFile.getFileName(),
                    expBreakpointFile.getFileName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expBreakpointFile != null ) {
                ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadFileById() throws Exception {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( fileName );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsR, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            actfile.getContent( downloadPath );
            Assert.assertEquals( actfile.getFileName(), actfile.getFileName() );
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
    private void testReadFileByPath() throws Exception {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( fileName );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstanceByPath( wsR,
                    "/" + fileName );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            actfile.getContent( downloadPath );
            Assert.assertEquals( actfile.getFileName(), actfile.getFileName() );
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
    private void testReadFileByVersion() throws Exception {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( fileName );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsR, fileId, 1, 0 );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            actfile.getContent( downloadPath );
            Assert.assertEquals( actfile.getFileName(), actfile.getFileName() );
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
    private void testReadFileByPathVersion() throws Exception {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( fileName );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstanceByPath( wsR,
                    "/" + fileName, 1, 0 );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            actfile.getContent( downloadPath );
            Assert.assertEquals( actfile.getFileName(), actfile.getFileName() );
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
    private void testGetSche() throws Exception {
        String scheName = "AuthWs_Read1723" + "_" + UUID.randomUUID();
        SiteWrapper rootSite = ScmInfo.getRootSite();
        SiteWrapper branchSite = ScmInfo.getBranchSite();
        String maxStayTime = "0d";
        BSONObject queryCond = null;
        ScmSchedule expSche = null;
        try {
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                    .is( scheName ).get();
            ScmScheduleContent content = new ScmScheduleCopyFileContent(
                    branchSite.getSiteName(), rootSite.getSiteName(),
                    maxStayTime, queryCond );
            String crond = "* * * * * ? 2029";
            expSche = ScmSystem.Schedule.create( sessionA, wsp.getName(),
                    ScheduleType.COPY_FILE, scheName, null, content, crond );

            ScmSchedule actSche = ScmSystem.Schedule.get( sessionR,
                    expSche.getId() );
            Assert.assertEquals( actSche.getName(), expSche.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expSche != null ) {
                ScmSystem.Schedule.delete( sessionA, expSche.getId() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListSche() throws Exception {
        ScmCursor< ScmScheduleBasicInfo > cursor = null;
        try {
            cursor = ScmSystem.Schedule.list( sessionR, new BasicBSONObject() );
            Assert.assertEquals( cursor != null, true );
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
    private void testCountFile() throws Exception {
        long i = 0;
        try {
            i = ScmFactory.File.countInstance( wsR, ScopeType.SCOPE_ALL,
                    new BasicBSONObject() );
            Assert.assertEquals( i >= 0, true );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
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

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
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

            rs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );

            ScmAuthUtils.checkPriority( site, username, passwd, role,
                    wsp.getName() );
            sessionR = ScmSessionUtils.createSession( site, username, passwd );
            wsR = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionR );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
