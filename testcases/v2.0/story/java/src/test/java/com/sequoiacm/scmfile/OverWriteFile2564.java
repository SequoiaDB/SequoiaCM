package com.sequoiacm.scmfile;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2564 :: 指定isOverwrite参数，上传/更新/查询/删除不带有批次的同名scm文件
 * @author fanyu
 * @Date:2019年8月21日
 * @version:1.0
 */
public class OverWriteFile2564 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String fileName = "file2564";
    private ScmId fileId;
    private File localPath;
    private int fileSize = 1024 * new Random().nextInt( 1024 );
    private String filePath;
    private String updateFilePath;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updateFilePath = localPath + File.separator + "localFile_"
                + ( fileSize + 1 ) + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updateFilePath, fileSize + 1 );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        prepareFile();
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        ScmFile scmFile = ScmFactory.File.createInstance( ws );
        scmFile.setFileName( fileName );
        scmFile.setContent( updateFilePath );
        // ScmUploadConf is null
        try {
            scmFile.save( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_EXIST ) {
                throw e;
            }
        }
        // overwrite is false
        try {
            scmFile.save( new ScmUploadConf( false ) );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_EXIST ) {
                throw e;
            }
        }
        // overwrite is true
        String newVal = fileName + "-new";
        scmFile.setAuthor( newVal );
        scmFile.setTitle( newVal );
        ScmTags scmTags = new ScmTags();
        scmTags.addTag( newVal );
        scmFile.setTags( scmTags );
        scmFile.setContent( updateFilePath );
        fileId = scmFile.save( new ScmUploadConf( true ) );
        // get scm file and check
        ScmFile actFile = ScmFactory.File.getInstance( ws, fileId );
        checkFile( actFile, newVal, fileSize + 1, updateFilePath, scmTags );
        // delete file
        ScmFactory.File.deleteInstance( ws, fileId, true );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareFile() throws ScmException {
        // create tags
        ScmTags scmTags = new ScmTags();
        scmTags.addTag( fileName );
        // create file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setTitle( fileName );
        file.setTags( scmTags );
        file.setContent( filePath );
        fileId = file.save();
    }

    private void checkFile( ScmFile file, String expVal, int expSize,
            String expFilePath, ScmTags expScmTags ) throws Exception {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertEquals( file.getFileName(), fileName );
            Assert.assertEquals( file.getAuthor(), expVal );
            Assert.assertEquals( file.getTitle(), expVal );
            Assert.assertEquals( file.getMimeType(), "text/plain" );
            Assert.assertEquals( file.getSize(), expSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getTags().toSet().toString(),
                    expScmTags.toSet().toString() );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            // check content
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( expFilePath ) );
        } catch ( AssertionError e ) {
            throw new Exception( "fileName = " + file.getFileName()
                    + "fileId = " + fileId.get(), e );
        }
    }
}
