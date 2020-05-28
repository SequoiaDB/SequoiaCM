package com.sequoiacm.batch;

import java.util.List;

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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:attach file to the batch,update content by the file,than detach
 * file by batch testlink-case:SCM-2089
 *
 * @author wuyan
 * @Date 2018.07.16
 * @version 1.00
 */

public class DetachFileByBatch2089 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private byte[] writeData = new byte[ 1024 * 3 ];
    private byte[] updateData = new byte[ 1024 * 2 ];
    private ScmId batchId = null;
    private ScmId fileId = null;
    private String fileName = "file_batch_2089";
    private String batchName = "batch_2089";

    @BeforeClass()
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        // clean batch
        BSONObject tagBson = new BasicBSONObject( "tags", "tag2086" );
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.listInstance( ws,
                new BasicBSONObject( "tags", tagBson ) );
        while ( cursor.hasNext() ) {
            ScmBatchInfo info = cursor.getNext();
            ScmId batchId = info.getId();
            ScmFactory.Batch.deleteInstance( ws, batchId );
        }
        cursor.close();

        // create file
        fileId = VersionUtils.createFileByStream( ws, fileName, writeData );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        updateFile( ws, fileId );
        batchId = createBatchAndAttachFile( ws, batchName, fileId );
        checkBatchInfoByFile( ws, fileId, batchId );

        // detach file by batch , than delete batch
        detachFileAndDeteleBatch( ws, batchId, fileId );
        checkDetachFileAndDeleteResult( ws, batchId, fileId );

        // delete file
        deleteFileAndCheckResult( ws, fileId );
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                try {
                    ScmFactory.Batch.deleteInstance( ws, batchId );
                } catch ( ScmException e ) {
                    Assert.assertEquals( e.getError(),
                            ScmError.BATCH_NOT_FOUND );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void updateFile( ScmWorkspace ws, ScmId fileId )
            throws ScmException {
        int updateTimes = 10;
        for ( int i = 0; i < updateTimes; i++ ) {
            VersionUtils.updateContentByStream( ws, fileId, updateData );
        }
    }

    private ScmId createBatchAndAttachFile( ScmWorkspace ws, String batchName,
            ScmId fileId ) throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        ScmId batchId = batch.save();
        batch.attachFile( fileId );

        // add tags
        ScmTags tags = new ScmTags();
        tags.addTag( "tag2086" );
        batch.setTags( tags );
        return batchId;
    }

    private void checkBatchInfoByFile( ScmWorkspace ws, ScmId fileId,
            ScmId batchId ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmId getBatchId = file.getBatchId();
        Assert.assertEquals( getBatchId, batchId );

        // batch contains a file
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        List< ScmFile > files = batch.listFiles();
        Assert.assertEquals( files.size(), 1 );
        Assert.assertEquals( file.toString(), files.get( 0 ).toString() );
    }

    private void detachFileAndDeteleBatch( ScmWorkspace ws, ScmId batchId,
            ScmId fileId ) throws ScmException {
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        batch.detachFile( fileId );
        ScmFactory.Batch.deleteInstance( ws, batchId );
    }

    private void checkDetachFileAndDeleteResult( ScmWorkspace ws, ScmId batchId,
            ScmId fileId ) throws ScmException {
        // the batch is no exist
        try {
            ScmFactory.Batch.getInstance( ws, batchId );
            Assert.fail( "get batch must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.BATCH_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-250  actError:" + e.getError()
                        + e.getMessage() );
            }
        }

        // all version file exist
        BSONObject findCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.toString() )
                .get();
        long fileCount = ScmFactory.File.countInstance( ws, ScopeType.SCOPE_ALL,
                findCondition );
        long fileNums = 11;
        Assert.assertEquals( fileCount, fileNums );
    }

    private void deleteFileAndCheckResult( ScmWorkspace ws, ScmId fileId )
            throws ScmException {
        ScmFactory.File.deleteInstance( ws, fileId, true );
        BSONObject findCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.toString() )
                .get();
        long fileCount = ScmFactory.File.countInstance( ws, ScopeType.SCOPE_ALL,
                findCondition );
        // all version file has deleted,the count is 0
        Assert.assertEquals( fileCount, 0 );
    }
}