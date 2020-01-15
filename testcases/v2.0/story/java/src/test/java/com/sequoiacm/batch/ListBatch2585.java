package com.sequoiacm.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2585:不指定排序分页列取批次列表
 * @author fanyu
 * @Date:2019年8月28日
 * @version:1.0
 */
public class ListBatch2585 extends TestScmBase {
    private AtomicInteger expSuccessTestCount = new AtomicInteger( 0 );
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private int batchNum = 100;
    private List< ScmId > batchIdList = new ArrayList<>();
    private String batchNamePrefix = "batch2585";
    private String tag = "tag2585";
    private BSONObject filter;
    private List< ScmBatchInfo > batchList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        //prepare batch
        for ( int i = 0; i < batchNum; i++ ) {
            String batchName = batchNamePrefix + "-" + i;
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.Batch.NAME ).is( batchName ).get();
            if ( ScmFactory.Batch.countInstance( ws, cond ) != 0 ) {
                ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                        .listInstance( ws, cond );
                while ( cursor.hasNext() ) {
                    ScmFactory.Batch
                            .deleteInstance( ws, cursor.getNext().getId() );
                }
            }
            ScmBatch scmBatch = ScmFactory.Batch.createInstance( ws );
            scmBatch.addTag( tag );
            scmBatch.setName( batchName );
            batchIdList.add( scmBatch.save() );
        }
        filter = ScmQueryBuilder.start( ScmAttributeName.Batch.TAGS ).is( tag )
                .get();
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                .listInstance( ws, filter );
        while ( cursor.hasNext() ) {
            batchList.add( cursor.getNext() );
        }
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateRangData() throws Exception {
        BSONObject orderby = ScmQueryBuilder
                .start( ScmAttributeName.Batch.TAGS + "-" + "inexistences" )
                .is( 1 ).get();
        return new Object[][] {
                //filter  skip   limit initScmFiles  sortnameArr  typeArr
                //orderby:单个字段 正序
                //skip=0  limit=1
                { filter, null, 0, 1, batchList },
                //skip>0 limt=50
                { filter, null, 1, 50, batchList },
                //skip>10 limt=50
                { filter, null, 10, 100, batchList },
                //skip == batchList.size
                { filter, null, batchList.size(), 10, batchList },
                //skip > batchList.size
                { filter, null, batchList.size() + 1, 10,
                        new ArrayList< ScmFileBasicInfo >() },
                //limit > batchList.size
                { filter, null, 0, batchList.size() + 1, batchList },
                //limit = -1
                { filter, null, 10, -1, batchList },
                //orderby:不存在的字段
                { filter, orderby, 0, 10, batchList }, };
    }

    @Test(dataProvider = "dataProvider")
    private void test( BSONObject filter, BSONObject orderby, long skip,
            long limit, List< ScmBatchInfo > list ) throws Exception {
        int actPageSize = 0;
        long tmpSkip = skip;
        int totalNum = 0;
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session );
            while ( tmpSkip < list.size() ) {
                ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                        .listInstance( ws, filter, orderby, tmpSkip, limit );
                int count = 0;
                while ( cursor.hasNext() ) {
                    ScmBatchInfo act = cursor.getNext();
                    try {
                        Assert.assertNotNull( act.getId() );
                        Assert.assertEquals( act.getFilesCount(), 0 );
                        Assert.assertEquals(
                                act.getName().contains( batchNamePrefix ),
                                true );
                        count++;
                    } catch ( AssertionError e ) {
                        throw new Exception(
                                "filter = " + filter + ",orderby = " + orderby
                                        + ",skip = " + skip + ",limit = "
                                        + limit + "，act = " + act, e );
                    }
                }
                if ( limit == 0 || count == 0 ) {
                    break;
                }
                tmpSkip += count;
                totalNum += count;
                actPageSize++;
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        try {
            int size = list.size();
            if ( skip < size && limit != 0 ) {
                Assert.assertEquals( totalNum, size - skip );
                if ( limit != -1 ) {
                    Assert.assertEquals( actPageSize,
                            ( int ) Math.ceil( ( ( double ) size / limit ) ) );
                } else {
                    Assert.assertEquals( actPageSize, 1 );
                }
            } else {
                Assert.assertEquals( totalNum, 0, "orderby = " + orderby );
                Assert.assertEquals( actPageSize, 0 );
            }
        } catch ( AssertionError e ) {
            throw new Exception(
                    "filter = " + filter + ",orderby = " + orderby + ",skip = "
                            + skip + ",limit = " + limit, e );
        }
        expSuccessTestCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( expSuccessTestCount.get() == 8 || TestScmBase.forceClear ) {
                for ( ScmId batchId : batchIdList ) {
                    ScmFactory.Batch.deleteInstance( ws, batchId );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}


