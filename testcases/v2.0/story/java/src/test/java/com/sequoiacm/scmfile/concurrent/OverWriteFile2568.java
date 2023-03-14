package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-2568 :: 并发上传文件和解除批次
 * @author fanyu
 * @Date:2019年8月22日
 * @version:1.0
 */
public class OverWriteFile2568 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String batchName = "batch2568";
    private String fileName = "file2568";
    private List< ScmId > origFileIdList = new ArrayList<>();
    private List< ScmId > fileIdList = new CopyOnWriteArrayList<>();
    private ScmBatch batch;
    private ScmId batchId;
    private File localPath;
    private int fileNum = 10;
    private int fileSize = new Random().nextInt( 1024 ) * 1024;
    private int updateFileSize = fileSize + 1;
    private String filePath;
    private String updateFilePath;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updateFilePath = localPath + File.separator + "localFile_"
                + updateFileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updateFilePath, updateFileSize );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.Batch.NAME )
                .is( batchName ).get();
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.listInstance( ws,
                cond );
        while ( cursor.hasNext() ) {
            ScmFactory.Batch.deleteInstance( ws, cursor.getNext().getId() );
        }
        cursor.close();
        // create batch
        // create batch and attach file
        batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = prepareFile( fileName + "-" + i );
            batch.attachFile( fileId );
            origFileIdList.add( fileId );
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < fileNum; i++ ) {
            threadExec.addWorker( new OverWriteFile( fileName + "-" + i ) );
            threadExec.addWorker( new DetachFile( origFileIdList.get( i ) ) );
        }
        threadExec.run();
        // check result
        Assert.assertEquals( fileIdList.size(), fileNum,
                "fileIdList = " + fileIdList.toString() );
        for ( ScmId fileId : fileIdList ) {
            ScmFile actFile = ScmFactory.File.getInstance( ws, fileId );
            checkFile( actFile, fileName, fileSize + 1, updateFilePath );
        }
        // get batch and check 覆盖后的文件与批次关系解除
        ScmBatch scmBatch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( scmBatch.listFiles().size(), 0,
                scmBatch.toString() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmId prepareFile( String fileName ) throws ScmException {
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
        return file.save();
    }

    private void checkFile( ScmFile file, String expVal, int expSize,
            String expFilePath ) throws Exception {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertTrue( file.getFileName().contains( expVal ) );
            Assert.assertEquals( file.getSize(), expSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getTags().toSet().size(), 0 );
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
                    + ",fileId = " + file.getFileId().get(), e );
        }
    }

    private class OverWriteFile {
        private String fileName;
        private ScmSession session;
        private ScmWorkspace ws;
        private ScmFile scmFile;

        public OverWriteFile( String filName ) throws ScmException {
            this.fileName = filName;
            this.session = ScmSessionUtils.createSession( site );
            this.ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            this.scmFile = ScmFactory.File.createInstance( ws );
            this.scmFile.setFileName( this.fileName );
            this.scmFile.setContent( updateFilePath );
        }

        @ExecuteOrder(step = 1)
        private void overwriteScmFile() throws ScmException {
            // 这里是覆盖文件
            try {
                fileIdList
                        .add( this.scmFile.save( new ScmUploadConf( true ) ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DetachFile {
        private ScmId fileId;

        public DetachFile( ScmId fileId ) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void detachFile() throws ScmException {
            ScmSession session = ScmSessionUtils.createSession( site );
            try {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                batch.detachFile( fileId );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_NOT_IN_BATCH ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
