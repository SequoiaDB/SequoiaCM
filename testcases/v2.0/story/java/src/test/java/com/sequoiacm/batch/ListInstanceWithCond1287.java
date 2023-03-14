package com.sequoiacm.batch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @FileName SCM-1287: 创建多个所有属性相同的批次
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class ListInstanceWithCond1287 extends TestScmBase {
    private final int batchNum = 5;
    private final String batchName = "batch1287";
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );
    }

    // TODO: fail for SEQUOIACM-233
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        for ( int i = 0; i < batchNum; ++i ) {
            ScmBatch batch = ScmFactory.Batch.createInstance( ws );
            batch.setName( batchName );
            ScmTags tags = new ScmTags();
            tags.addTag( "tag1287我是标签_" + i );
            batch.setTags( tags );
            batch.save();
        }

        int total = 0;
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.listInstance( ws,
                new BasicBSONObject( "name", batchName ) );
        while ( cursor.hasNext() ) {
            ScmBatchInfo info = cursor.getNext();
            Assert.assertEquals( batchName, info.getName() );
            ++total;
        }
        cursor.close();
        Assert.assertEquals( total, batchNum, "wrong batch num" );

        total = 0;
        BSONObject tagBson = ScmQueryBuilder.start( "tags" )
                .in( "tag1287我是标签_0" ).get();
        cursor = ScmFactory.Batch.listInstance( ws, tagBson );
        while ( cursor.hasNext() ) {
            ScmBatchInfo info = cursor.getNext();
            ScmId batchId = info.getId();
            ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
            Assert.assertEquals(
                    batch.getTags().toSet().contains( "tag1287我是标签_0" ), true,
                    batch.getTags().toSet().toString() );
            ++total;
        }
        cursor.close();
        Assert.assertEquals( total, 1, "wrong batch num" );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                        .listInstance( ws,
                                new BasicBSONObject( "name", batchName ) );
                while ( cursor.hasNext() ) {
                    ScmId batchId = cursor.getNext().getId();
                    ScmFactory.Batch.deleteInstance( ws, batchId );
                }
                cursor.close();
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }
}