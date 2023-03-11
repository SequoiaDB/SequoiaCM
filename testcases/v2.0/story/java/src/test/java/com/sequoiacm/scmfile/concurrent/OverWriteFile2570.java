package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testresource.SkipTestException;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-2570 :: 并发断点文件覆盖scm文件和scm文件覆盖scm文件
 * @author fanyu
 * @Date:2019年8月22日
 * @version:1.0
 */
public class OverWriteFile2570 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String batchName = "batch2570";
    private String fileNamePre = "file2570";
    private List< String > fileNameList = new ArrayList<>();
    private List< ScmId > origFileIdList = new ArrayList<>();
    private List< ScmId > newFileIdList = new ArrayList<>();
    private List< ScmBreakpointFile > breakpointFiles = new CopyOnWriteArrayList<>();
    private ScmBatch batch;
    private ScmId batchId;
    private File localPath;
    private int fileNum = 10;
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
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        for ( SiteWrapper tmpSite : sites ) {
            if ( tmpSite.getDataType()
                    .equals( ScmType.DatasourceType.SEQUOIADB ) ) {
                site = tmpSite;
                break;
            }
        }
        if ( site == null ) {
            throw new SkipTestException(
                    "Upload BreakpointFile is not support in hdfs(hbase)" );
        }
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
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
            String fileName = fileNamePre + "-" + i;
            ScmId fileId = prepareFile( fileName );
            fileNameList.add( fileName );
            origFileIdList.add( fileId );
            batch.attachFile( fileId );
            breakpointFiles.add( prepareBreakpointFile( fileName ) );
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < fileNum; i++ ) {
            threadExec.addWorker( new OverWriteFile( fileNameList.get( i ) ) );
            threadExec.addWorker( new BreakpointFile2ScmFile(
                    fileNameList.get( i ), breakpointFiles.get( i ) ) );
        }
        threadExec.run();
        // check result
        String newVal = fileNamePre + "-new";
        ScmTags scmTags = new ScmTags();
        scmTags.addTag( newVal );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile actFile = ScmFactory.File.getInstanceByPath( ws,
                    "/" + fileNameList.get( i ) );
            newFileIdList.add( actFile.getFileId() );
            checkFile( actFile, fileNamePre, fileSize + 1, updateFilePath,
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
                for ( ScmBreakpointFile breakpointFile : breakpointFiles ) {
                    ScmFactory.BreakpointFile.deleteInstance( ws,
                            breakpointFile.getFileName() );
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

    private ScmBreakpointFile prepareBreakpointFile( String fileName )
            throws IOException, ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        InputStream inputStream = new FileInputStream( updateFilePath );
        breakpointFile.upload( inputStream );
        inputStream.close();
        return breakpointFile;
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
        } catch ( AssertionError e ) {
            throw new Exception( "fileName = " + file.getFileName()
                    + ",fileId = " + file.getFileId().get(), e );
        }
    }

    private class OverWriteFile {
        private String fileName;

        public OverWriteFile( String fileName ) {
            this.fileName = fileName;
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
                scmFile.setContent( updateFilePath );
                // overwrite is true
                String newVal = this.fileName + "-new";
                scmFile.setAuthor( newVal );
                scmFile.setTitle( newVal );
                ScmTags scmTags = new ScmTags();
                scmTags.addTag( newVal );
                scmFile.setTags( scmTags );
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

    private class BreakpointFile2ScmFile {
        private String fileName;
        private ScmBreakpointFile scmBreakpointFile;

        public BreakpointFile2ScmFile( String fileName,
                ScmBreakpointFile scmBreakpointFile ) {
            this.fileName = fileName;
            this.scmBreakpointFile = scmBreakpointFile;
        }

        @ExecuteOrder(step = 1)
        private void breakpointFile2ScmFile() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile scmFile = ScmFactory.File.createInstance( ws );
                scmFile.setFileName( this.fileName );
                scmFile.setContent( this.scmBreakpointFile );
                // overwrite is true
                String newVal = this.fileName + "-new";
                scmFile.setAuthor( newVal );
                scmFile.setTitle( newVal );
                ScmTags scmTags = new ScmTags();
                scmTags.addTag( newVal );
                scmFile.setTags( scmTags );
                scmFile.save( new ScmUploadConf( true ) );
                breakpointFiles.remove( scmBreakpointFile );
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
