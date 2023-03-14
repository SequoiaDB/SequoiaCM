package com.sequoiacm.scmfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
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
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
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
 * @Description: SCM-2676 :: 用户有相关的权限，覆盖文件
 * @author fanyu
 * @Date:2019年10月24日
 * @version:1.0
 */
public class OverWriteFile2676 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String fileName = "file2676";
    private ScmId fileId;
    private String scmDir = "/dir2676";
    private String scmDirId;
    private File localPath;
    private int fileSize = 1024 * new Random().nextInt( 1024 );
    private String filePath;
    private String username = "user2676";
    private String rolename = "role2676";
    private ScmUser scmUser;
    private ScmRole scmRole;
    private String passwd = "2676";
    private String batchName = "batch2676";
    private ScmId batchId;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // prepare user
        prepareUser();
        // prepare scm file
        prepareScmFile();
    }

    @Test
    private void test() throws Exception {
        // 有目录的create、delete权限，工作区的update权限
        ScmResource dirResource = ScmResourceFactory
                .createDirectoryResource( wsp.getName(), scmDir );
        ScmResource wsResource = ScmResourceFactory
                .createWorkspaceResource( wsp.getName() );
        grantPrivilege( dirResource, ScmPrivilegeType.CREATE );
        grantPrivilege( wsResource, ScmPrivilegeType.UPDATE );
        grantPrivilege( dirResource, ScmPrivilegeType.DELETE );
        // over write scm file
        overwriteFile();
        checkFile();
        // 有目录的create、delete权限，无工作区的update权限
        // revoke Privilege
        revokePrivilege( wsResource, ScmPrivilegeType.UPDATE );
        // over write scm file again
        try {
            overwriteFile();
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }
        // 有目录的delete权限，有工作区的update权限
        revokePrivilege( dirResource, ScmPrivilegeType.CREATE );
        grantPrivilege( wsResource, ScmPrivilegeType.UPDATE );
        try {
            overwriteFile();
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Directory.deleteInstance( ws, scmDir );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void overwriteFile() throws ScmException, FileNotFoundException {
        ScmSession session1 = null;
        try {
            session1 = ScmSessionUtils.createSession( site, username, passwd );
            ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session1 );
            ScmFile scmFile = ScmFactory.File.createInstance( ws1 );
            scmFile.setFileName( fileName );
            scmFile.setAuthor( fileName );
            scmFile.setDirectory( scmDirId );
            scmFile.setContent( new FileInputStream( new File( filePath ) ) );
            fileId = scmFile.save( new ScmUploadConf( true ) );
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
        }
    }

    private void checkFile() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileName(), fileName );
            Assert.assertEquals( file.getAuthor(), fileName );
            Assert.assertEquals( file.getTitle(), "" );
            Assert.assertEquals( file.getSize(), fileSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getTags().toSet().size(), 0 );
            Assert.assertEquals( file.getUser(), username );
            Assert.assertNotNull( file.getCreateTime().getTime() );
            Assert.assertNull( file.getBatchId() );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            // check content
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( filePath ) );
        } catch ( AssertionError e ) {
            throw new Exception( "scmFile = " + file.toString(), e );
        }
    }

    private void prepareScmFile() throws ScmException {
        BSONObject cond1 = ScmQueryBuilder.start( ScmAttributeName.Batch.NAME )
                .is( batchName ).get();
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.listInstance( ws,
                cond1 );
        while ( cursor.hasNext() ) {
            ScmFactory.Batch.deleteInstance( ws, cursor.getNext().getId() );
        }
        cursor.close();

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // prepare scm dir
        if ( ScmFactory.Directory.isInstanceExist( ws, scmDir ) ) {
            ScmFactory.Directory.deleteInstance( ws, scmDir );
        }
        ScmDirectory scmDirectory = ScmFactory.Directory.createInstance( ws,
                scmDir );
        scmDirId = scmDirectory.getId();
        // create file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setContent( filePath );
        file.setDirectory( scmDirectory.getId() );
        fileId = file.save();
        // create batch and attach file
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();
        // batch.attachFile(fileId);
    }

    private void prepareUser() throws Exception {
        try {
            ScmFactory.Role.deleteRole( session, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        try {
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        scmUser = ScmFactory.User.createUser( session, username,
                ScmUserPasswordType.LOCAL, passwd );
        scmRole = ScmFactory.Role.createRole( session, rolename, null );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( scmRole );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    private void grantPrivilege( ScmResource rs, ScmPrivilegeType privileges )
            throws Exception {
        ScmFactory.Role.grantPrivilege( session, scmRole, rs, privileges );
        ScmAuthUtils.checkPriority( site, username, passwd, scmRole,
                wsp.getName() );
    }

    private void revokePrivilege( ScmResource rs, ScmPrivilegeType privileges )
            throws Exception {
        ScmFactory.Role.revokePrivilege( session, scmRole, rs, privileges );
        ScmAuthUtils.checkPriority( site, username, passwd, scmRole,
                wsp.getName() );
    }
}
