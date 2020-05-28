package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-2677::并发使用流覆盖和使用文件覆盖同一个文件
 * @author fanyu
 * @Date:2019年10月24日
 * @version:1.0
 */
public class OverWriteFile2677 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String batchName = "batch2677";
    private String fileNamePre = "file2677";
    private List< String > fileNameList = new ArrayList< String >();
    private List< ScmId > newFileIdList = new ArrayList< ScmId >();
    private ScmBatch batch;
    private ScmId batchId;
    private File localPath;
    private int fileNum = 10;
    private int fileSize = 1024 * new Random().nextInt( 1024 );
    private String filePath;
    private String updateFilePath1;
    private String updateFilePath2;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updateFilePath1 = localPath + File.separator + "localFileA_"
                + new Random().nextInt( 1024 * 1024 ) + ".txt";
        updateFilePath2 = localPath + File.separator + "localFileB_"
                + new Random().nextInt( 1024 * 1024 ) + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updateFilePath1,
                new Random().nextInt( 1024 * 1024 ) );
        TestTools.LocalFile.createFile( updateFilePath2,
                new Random().nextInt( 1024 * 1024 ) );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // clean batch
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.Batch.NAME )
                .is( batchName ).get();
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.listInstance( ws,
                cond );
        while ( cursor.hasNext() ) {
            ScmFactory.Batch.deleteInstance( ws, cursor.getNext().getId() );
        }
        cursor.close();
        // create batch and attach file
        batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();
        for ( int i = 0; i < fileNum; i++ ) {
            String fileName = fileNamePre + "-" + i;
            ScmId fileId = prepareFile( fileName );
            fileNameList.add( fileName );
            batch.attachFile( fileId );
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        String flag1 = fileNamePre + "-flag-1";
        String flag2 = fileNamePre + "-flag-2";
        for ( int i = 0; i < fileNum; i++ ) {
            threadExec.addWorker(
                    new OverWriteFileByPath( fileNameList.get( i ), flag1 ) );
            threadExec.addWorker(
                    new OverWriteFileByStream( fileNameList.get( i ), flag2 ) );
        }
        threadExec.run();
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile actFile = ScmFactory.File.getInstanceByPath( ws,
                    "/" + fileNameList.get( i ) );
            newFileIdList.add( actFile.getFileId() );
            if ( actFile.getAuthor().equals( flag1 ) ) {
                checkFile( actFile, flag1,
                        ( int ) new File( updateFilePath1 ).length(),
                        updateFilePath1 );
            } else {
                checkFile( actFile, flag2,
                        ( int ) new File( updateFilePath2 ).length(),
                        updateFilePath2 );
            }
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

    private void checkFile( ScmFile file, String author, int expSize,
            String expFilePath ) throws Exception {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getAuthor(), author );
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
            throw new Exception( "scmFile = " + file.toString(), e );
        }
    }

    private class OverWriteFileByPath {
        private String fileName;
        private String flag;

        public OverWriteFileByPath( String fileName, String flag ) {
            this.fileName = fileName;
            this.flag = flag;
        }

        @ExecuteOrder(step = 1)
        private void overwriteScmFile() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile scmFile = ScmFactory.File.createInstance( ws );
                scmFile.setFileName( this.fileName );
                scmFile.setAuthor( flag );
                scmFile.setContent( updateFilePath1 );
                scmFile.save( new ScmUploadConf( true ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_EXIST ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class OverWriteFileByStream {
        private String fileName;
        private String flag;

        public OverWriteFileByStream( String fileName, String flag ) {
            this.fileName = fileName;
            this.flag = flag;
        }

        @ExecuteOrder(step = 1)
        private void overwriteScmFile()
                throws ScmException, FileNotFoundException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile scmFile = ScmFactory.File.createInstance( ws );
                scmFile.setFileName( this.fileName );
                scmFile.setAuthor( this.flag );
                scmFile.setContent(
                        new FileInputStream( new File( updateFilePath2 ) ) );
                scmFile.save( new ScmUploadConf( true ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_EXIST ) {
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
