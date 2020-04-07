package com.sequoiacm.batch.concurrent;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
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

/**
 * test content:create multiple batchs concurrent
 * testlink-case:SCM-2081
 *
 * @author wuyan
 * @Date 2018.07.16
 * @version 1.00
 */

public class CreateMutiBatchs2081 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private String authorName = "file2081";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private byte[] writeData = new byte[ 1024 * 3 ];
    private LinkedBlockingDeque< ScmId > batchIdQue = new
            LinkedBlockingDeque< ScmId >();

    @BeforeClass()
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsp = ScmInfo.getWs();

        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject tagBson = new BasicBSONObject( "tags", "tag2081" );
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
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        List< CreateBatchThread > createBatchs = new ArrayList<>( 100 );
        String batchName = "";
        String fileName = "";
        new Random().nextBytes( writeData );
        for ( int i = 0; i < 100; i++ ) {
            fileName = "file2081_" + i;
            byte[] writeData = new byte[ 1024 * 2 ];
            ScmId fileId = createFile( ws, fileName, writeData );

            batchName = "batch2081_" + i;
            createBatchs.add( new CreateBatchThread( batchName, fileId ) );
        }

        for ( CreateBatchThread createBatch : createBatchs ) {
            createBatch.start();
        }

        for ( CreateBatchThread createBatch : createBatchs ) {
            Assert.assertTrue( createBatch.isSuccess(),
                    createBatch.getErrorMsg() );
        }
        checkBatch( ws );
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

    private ScmId createFile( ScmWorkspace ws, String fileName, byte[] data )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        file.setMimeType( fileName + ".txt" );
        ScmId fileId = file.save();
        return fileId;
    }

    private void checkBatchInfoByFile( ScmWorkspace ws, ScmId fileId,
            ScmId batchId ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmId getBatchId = file.getBatchId();
        Assert.assertEquals( getBatchId, batchId );
    }

    private void checkBatch( ScmWorkspace ws )
            throws ScmException, InterruptedException {
        int expBatchNum = 100;
        Assert.assertEquals( batchIdQue.size(), expBatchNum,
                "batch nums is error!" );

        @SuppressWarnings("rawtypes")
        Iterator iterator = batchIdQue.iterator();
        while ( iterator.hasNext() ) {
            ScmId batchId = ( ScmId ) iterator.next();
            ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );

            //each batch contains a file
            List< ScmFile > files = batch.listFiles();
            Assert.assertEquals( files.size(), 1 );
        }
    }

    private void deleteBatchAndCheckResult( ScmWorkspace ws )
            throws InterruptedException, ScmException {
        while ( !batchIdQue.isEmpty() ) {
            ScmId batchId = batchIdQue.take();

            //check delete result
            ScmFactory.Batch.deleteInstance( ws, batchId );
            try {
                ScmFactory.Batch.getInstance( ws, batchId );
                Assert.fail( "get batch must bu fail!" );
            } catch ( ScmException e ) {
                if ( ScmError.BATCH_NOT_FOUND != e.getError() ) {
                    Assert.fail( "expErrorCode:-250  actError:" + e.getError() +
                            e.getMessage() );
                }
            }
        }

        //check file by Batch
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        long remainFileNum = ScmFactory.File
                .countInstance( ws, ScopeType.SCOPE_CURRENT, fileCondition );
        int expFileNum = 0;
        Assert.assertEquals( remainFileNum, expFileNum );
    }

    private class CreateBatchThread extends TestThreadBase {
        String batchName;
        ScmId fileId;

        public CreateBatchThread( String batchName, ScmId fileId ) {
            this.batchName = batchName;
            this.fileId = fileId;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getSite() );
            try {
                ScmWorkspace wsTmp = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBatch batch = ScmFactory.Batch.createInstance( wsTmp );
                batch.setName( batchName );
                ScmId batchId = batch.save();
                ScmTags tags = new ScmTags();
                tags.addTag( "tag2081" );
                batch.setTags( tags );
                batch.attachFile( fileId );
                batchIdQue.offer( batchId );
                // get batchId by file
                checkBatchInfoByFile( wsTmp, fileId, batchId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}