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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-2573:并发覆盖上传文件和删除文件
 * @author fanyu
 * @Date:2019年8月23日
 * @version:1.0
 */
public class OverWriteFile2573 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String dirPath = "/dir2573/";
    private String batchName = "batch2573";
    private String fileNamePre = "file2573";
    private ScmDirectory scmDirectory;
    private List< String > fileNameList = new ArrayList<>();
    private List< ScmId > origFileIdList = new ArrayList<>();
    private List< ScmId > newFileIdList = new CopyOnWriteArrayList<>();
    private ScmBatch batch;
    private ScmId batchId;
    private File localPath;
    private int fileNum = 10;
    private int fileSize = 1024 * new Random().nextInt( 1024 );
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
        // create directory
        scmDirectory = ScmFactory.Directory.createInstance( ws, dirPath );
        // create batch and attach file
        batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();
        for ( int i = 0; i < fileNum; i++ ) {
            String fileName = fileNamePre + "-" + i;
            ScmId fileId = prepareFile( fileName );
            fileNameList.add( fileName );
            origFileIdList.add( fileId );
            if ( i / 2 == 0 ) {
                batch.attachFile( fileId );
            }
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < fileNum; i++ ) {
            threadExec.addWorker( new OverWriteFile( fileNameList.get( i ) ) );
            threadExec.addWorker( new DeleteFile( i ) );
        }
        threadExec.run();
        // check result
        String newVal = fileNamePre + "-new";
        ScmTags scmTags = new ScmTags();
        scmTags.addTag( newVal );
        for ( int i = 0; i < newFileIdList.size(); i++ ) {
            ScmFile actFile = ScmFactory.File.getInstance( ws,
                    newFileIdList.get( i ) );
            checkFile( actFile, fileNamePre, updateFileSize, updateFilePath,
                    scmTags );
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
                for ( ScmId fileId : newFileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                ScmFactory.Directory.deleteInstance( ws, dirPath );
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
        file.setDirectory( scmDirectory );
        file.setTitle( fileName );
        file.setTags( scmTags );
        file.setContent( filePath );
        return file.save();
    }

    private void checkFile( ScmFile file, String expVal, int expSize,
            String expFilePath, ScmTags expScmTags ) throws Exception {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertTrue( file.getAuthor().contains( expVal ) );
            Assert.assertTrue( file.getAuthor().contains( expVal ) );
            Assert.assertEquals( file.getSize(), expSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getDirectory().getPath(), dirPath );
            Assert.assertEquals( file.getTags().toSet().size(),
                    expScmTags.toSet().size() );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            // check content
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( expFilePath ) );
        } catch ( Exception e ) {
            throw new Exception( "file = " + file.toString(), e );
        }
    }

    private class OverWriteFile {
        private String fileName;
        private ScmSession session;
        private ScmWorkspace ws;
        private ScmFile scmFile;

        public OverWriteFile( String fileName ) throws ScmException {
            this.fileName = fileName;
            this.session = ScmSessionUtils.createSession( site );
            this.ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            this.scmFile = ScmFactory.File.createInstance( ws );
            this.scmFile.setFileName( this.fileName );
            this.scmFile.setDirectory( scmDirectory );
            this.scmFile.setContent( updateFilePath );
            // overwrite is true
            String newVal = this.fileName + "-new";
            this.scmFile.setAuthor( newVal );
            this.scmFile.setTitle( newVal );
            ScmTags scmTags = new ScmTags();
            scmTags.addTag( newVal );
            this.scmFile.setTags( scmTags );
        }

        @ExecuteOrder(step = 1)
        private void overwriteScmFile() throws Exception {
            try {
                try {
                    newFileIdList
                            .add( scmFile.save( new ScmUploadConf( true ) ) );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.FILE_EXIST ) {
                        throw new Exception( "scmfile = " + scmFile.toString(),
                                e );
                    }
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFile {
        private int index;
        private ScmSession session;
        private ScmWorkspace ws;

        public DeleteFile( int index ) throws ScmException {
            this.index = index;
            this.session = ScmSessionUtils.createSession( site );
            this.ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
        }

        @ExecuteOrder(step = 1)
        private void deleteFile() throws ScmException {
            try {
                if ( index / 2 == 0 ) {
                    batch.detachFile( origFileIdList.get( index ) );
                }
                ScmFactory.File.deleteInstance( ws, origFileIdList.get( index ),
                        true );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_NOT_IN_BATCH
                        && e.getError() != ScmError.FILE_NOT_FOUND ) {
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
