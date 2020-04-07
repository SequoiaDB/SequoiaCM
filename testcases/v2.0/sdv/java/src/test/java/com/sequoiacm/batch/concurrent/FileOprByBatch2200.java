package com.sequoiacm.batch.concurrent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:create/delete/update files concurrently under the same batch
 * testlink-case:SCM-2200
 *
 * @author wuyan
 * @Date 2018.09.07
 * @version 1.00
 */
public class FileOprByBatch2200 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private String batchName = "batch2200";
    private ScmId batchId = null;
    private String authorName = "file2200";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private byte[] writeData = new byte[ 1024 * 3 ];
    private byte[] updateData = new byte[ 1024 * 2 ];
    private LinkedBlockingDeque< ScmId > fileIdQue = new LinkedBlockingDeque<
            ScmId >();

    @BeforeClass()
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsp = ScmInfo.getWs();

        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject tagBson = new BasicBSONObject( "tags", "tag2200" );
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.
                listInstance( ws, new BasicBSONObject( "tags", tagBson ) );
        while ( cursor.hasNext() ) {
            ScmBatchInfo info = cursor.getNext();
            ScmId batchId = info.getId();
            ScmFactory.Batch.deleteInstance( ws, batchId );
        }
        cursor.close();

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        batchId = createBatch( ws );
        new Random().nextBytes( writeData );
        int fileNums = 50;
        createFile( ws, writeData, fileNums );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        List< CreateFileThread > createFiles = new ArrayList<>( 20 );
        UpdateFileThread updateFiles = new UpdateFileThread();
        DeleteFileThread deleteFiles = new DeleteFileThread();
        new Random().nextBytes( updateData );

        for ( int i = 0; i < 20; i++ ) {
            String fileName = "file2200a_" + i;
            createFiles.add( new CreateFileThread( fileName ) );
        }

        for ( CreateFileThread createFile : createFiles ) {
            createFile.start();
        }

        updateFiles.start( 20 );
        deleteFiles.start( 20 );

        for ( CreateFileThread createFile : createFiles ) {
            Assert.assertTrue( createFile.isSuccess(),
                    createFile.getErrorMsg() );
        }

        Assert.assertTrue( updateFiles.isSuccess(), updateFiles.getErrorMsg() );
        Assert.assertTrue( deleteFiles.isSuccess(), deleteFiles.getErrorMsg() );

        checkFileNumByBatch( ws );
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                deleteBatchAndCheckResult( ws );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileContent( ScmWorkspace ws, ScmId fileId,
            byte[] filedata ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        // down file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        byte[] downloadData = outputStream.toByteArray();

        // check results
        VersionUtils.assertByteArrayEqual( downloadData, filedata );
    }

    private ScmId createBatch( ScmWorkspace ws ) throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        ScmId batchId = batch.save();
        ScmTags tags = new ScmTags();
        tags.addTag( "tag2200" );
        batch.setTags( tags );
        return batchId;
    }

    private void createFile( ScmWorkspace ws, byte[] data, int fileNums )
            throws ScmException {
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        String fileName = "file2200";
        for ( int i = 0; i < fileNums; i++ ) {
            String fileNameSub = fileName + "_" + i;
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( new ByteArrayInputStream( data ) );
            file.setFileName( fileNameSub );
            file.setAuthor( authorName );
            file.setTitle( "sequoiacm" );
            file.setMimeType( fileName + ".txt" );
            ScmId fileId = file.save();
            batch.attachFile( fileId );
            fileIdQue.offer( fileId );
        }
    }

    private void checkBatchInfoByFile( ScmWorkspace ws, ScmId fileId,
            ScmId batchId ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmId getBatchId = file.getBatchId();
        Assert.assertEquals( getBatchId, batchId );
    }

    private void checkFileNumByBatch( ScmWorkspace ws )
            throws ScmException, InterruptedException {
        int expFileNums = 50;
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        List< ScmFile > files = batch.listFiles();
        Assert.assertEquals( files.size(), expFileNums );
    }

    private void deleteBatchAndCheckResult( ScmWorkspace ws )
            throws InterruptedException, ScmException {
        //check delete result
        ScmFactory.Batch.deleteInstance( ws, batchId );

        //check file by Batch
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        long remainFileNum = ScmFactory.File
                .countInstance( ws, ScopeType.SCOPE_CURRENT, fileCondition );
        int expFileNum = 0;
        Assert.assertEquals( remainFileNum, expFileNum );
    }

    private class CreateFileThread extends TestThreadBase {
        String fileName;

        public CreateFileThread( String fileName ) {
            this.fileName = fileName;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getSite() );
            try {
                ScmWorkspace wsTmp = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( wsTmp );
                file.setContent( new ByteArrayInputStream( writeData ) );
                file.setFileName( fileName );
                file.setAuthor( authorName );
                file.setTitle( "sequoiacm" );
                file.setMimeType( fileName + ".txt" );
                ScmId fileId = file.save();
                ScmBatch batch = ScmFactory.Batch.getInstance( wsTmp, batchId );
                batch.attachFile( fileId );
                fileIdQue.offer( fileId );

                //check file content
                checkFileContent( wsTmp, fileId, writeData );
                // get batchId by file
                checkBatchInfoByFile( wsTmp, fileId, batchId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getSite() );
            try {
                ScmId fileId = fileIdQue.take();
                ScmWorkspace wsTmp = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( wsTmp, fileId );
                file.updateContent( new ByteArrayInputStream( updateData ) );

                //check file content
                checkFileContent( wsTmp, fileId, updateData );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getSite() );
            try {
                ScmId fileId = fileIdQue.take();
                ScmWorkspace wsTmp = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBatch batch = ScmFactory.Batch.getInstance( wsTmp, batchId );
                batch.detachFile( fileId );
                ScmFactory.File.deleteInstance( wsTmp, fileId, true );
                // the file is no exist
                try {
                    ScmFactory.File.getInstance( wsTmp, fileId );
                    Assert.fail( "get file must bu fail!" );
                } catch ( ScmException e ) {
                    //System.out.println("---remove file and get error is
                    // :"+e.getError());
                    if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                        Assert.fail( "expError:FILE NOT FOUND  actError:" +
                                e.getError() + e.getMessage() );
                    }
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
