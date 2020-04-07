package com.sequoiacm.scmfile;

import java.io.File;
import java.io.IOException;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2562:指定isOverwrite参数，上传/更新/查询/删除带有批次的同名scm文件（覆盖的文件大小为0）
 * @author fanyu
 * @Date:2019年8月21日
 * @version:1.0
 */
public class OverWriteFile2562 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String dirPath = "/dir2562/";
    private String batchName = "batch2562";
    private String fileName = "file2562";
    private ScmDirectory scmDirectory;
    private ScmId fileId;
    private ScmId batchId;
    private File localPath;
    private Random random = new Random();
    private int fileSize = 1024 * random.nextInt( 1024 );
    private int updateFileSize = 0;
    private String filePath;
    private String updateFilePath;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_C" + fileSize + ".txt";
        updateFilePath =
                localPath + File.separator + "localFile_U" + updateFileSize +
                        ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updateFilePath, updateFileSize );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.Batch.NAME )
                .is( batchName ).get();
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                .listInstance( ws, cond );
        while ( cursor.hasNext() ) {
            ScmFactory.Batch.deleteInstance( ws, cursor.getNext().getId() );
        }
        cursor.close();
        prepareFileWithBatch();
    }

    @Test
    private void test() throws Exception {
        ScmFile scmFile = ScmFactory.File.createInstance( ws );
        scmFile.setFileName( fileName );
        scmFile.setDirectory( scmDirectory );
        scmFile.setContent( updateFilePath );
        //ScmUploadConf is null
        try {
            scmFile.save( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_EXIST ) {
                throw e;
            }
        }
        //overwrite is false
        try {
            scmFile.save( new ScmUploadConf( false ) );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_EXIST ) {
                throw e;
            }
        }
        //overwrite is true
        String newVal = fileName + "-new";
        scmFile.setAuthor( newVal );
        scmFile.setTitle( newVal );
        ScmTags scmTags = new ScmTags();
        scmTags.addTag( newVal );
        scmFile.setTags( scmTags );
        fileId = scmFile.save( new ScmUploadConf( true ) );
        //get scm file by path and check
        ScmFile actFile = ScmFactory.File
                .getInstanceByPath( ws, dirPath + "/" + fileName );
        checkFile( actFile, newVal, updateFileSize, updateFilePath, scmTags );
        //get batch and check
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.listFiles().size(), 0, batch.toString() );
        //delete file
        ScmFactory.File.deleteInstance( ws, fileId, true );
        ScmFactory.Directory.deleteInstance( ws, dirPath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareFileWithBatch() throws ScmException {
        //create directory
        scmDirectory = ScmFactory.Directory.createInstance( ws, dirPath );
        //create tags
        ScmTags scmTags = new ScmTags();
        scmTags.addTag( fileName );
        //create file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setTitle( fileName );
        file.setTags( scmTags );
        file.setContent( filePath );
        file.setDirectory( scmDirectory );
        fileId = file.save();
        //create batch and attach file
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batch.setTags( scmTags );
        batchId = batch.save();
        batch.attachFile( fileId );
    }

    private void checkFile( ScmFile file, String expVal, int expSize,
            String expFilePath, ScmTags expScmTags ) throws Exception {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertEquals( file.getFileName(), fileName );
            Assert.assertEquals( file.getAuthor(), expVal );
            Assert.assertEquals( file.getTitle(), expVal );
            Assert.assertEquals( file.getDirectory().getPath(), dirPath );
            Assert.assertEquals( file.getMimeType(), "text/plain" );
            Assert.assertEquals( file.getSize(), expSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getTags().toSet().toString(),
                    expScmTags.toSet().toString() );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            file.getContent( downloadPath );
            // check content
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( expFilePath ) );
        } catch ( AssertionError e ) {
            throw new Exception( "file = " + file.toString(), e );
        }
    }
}
