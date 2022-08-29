package com.sequoiacm.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.testcommon.scmutils.ListUtils;

/**
 * @Description: SCM-2584 :: 逆序分页列取批次列表
 * @author fanyu
 * @Date:2019年8月28日
 * @version:1.0
 */
public class ListBatch2584 extends TestScmBase {
    private AtomicInteger expSuccessTestCount = new AtomicInteger( 0 );
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private int batchNum = 100;
    private List< ScmId > batchIdList = new ArrayList<>();
    private String batchNamePrefix = "batch2584";
    private String tag = "tag2584";
    private BSONObject filter;
    private List< ScmBatchInfo > batchList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // prepare batch
        for ( int i = 0; i < batchNum; i++ ) {
            String batchName = batchNamePrefix + "-" + i;
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.Batch.NAME ).is( batchName ).get();
            if ( ScmFactory.Batch.countInstance( ws, cond ) != 0 ) {
                ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                        .listInstance( ws, cond );
                while ( cursor.hasNext() ) {
                    ScmFactory.Batch.deleteInstance( ws,
                            cursor.getNext().getId() );
                }
            }
            ScmBatch scmBatch = ScmFactory.Batch.createInstance( ws );
            scmBatch.addTag( tag );
            scmBatch.setName( batchName );
            batchIdList.add( scmBatch.save() );
        }
        filter = ScmQueryBuilder.start( ScmAttributeName.Batch.TAGS ).is( tag )
                .get();
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.listInstance( ws,
                filter );
        while ( cursor.hasNext() ) {
            batchList.add( cursor.getNext() );
        }
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateRangData() throws Exception {
        BSONObject positive = ScmQueryBuilder
                .start( ScmAttributeName.Batch.NAME ).is( -1 ).get();
        BSONObject dPositive = ScmQueryBuilder
                .start( ScmAttributeName.Batch.CREATE_TIME ).is( -1 )
                .and( ScmAttributeName.Batch.NAME ).is( -1 ).get();

        String[] sortNameAtr1 = new String[] { "name" };
        String[] sortNameAtr2 = new String[] { "createTime", "name" };
        boolean[] typeAtr1 = new boolean[] { false };
        boolean[] typeAtr2 = new boolean[] { false, false };
        return new Object[][] {
                // filter skip limit initScmFiles sortnameArr typeArr
                // orderby:单个字段 逆序
                // skip=0 limit=1
                { filter, positive, 0, 1, batchList, sortNameAtr1, typeAtr1 },
                // skip>0 limt=50
                { filter, positive, 1, 50, batchList, sortNameAtr1, typeAtr1 },
                // skip>10 limt=50
                { filter, positive, 10, 100, batchList, sortNameAtr1,
                        typeAtr1 },
                // skip == batchList.size
                { filter, positive, batchList.size(), 10, batchList,
                        sortNameAtr1, typeAtr1 },
                // skip > batchList.size
                { filter, positive, batchList.size() + 1, 10,
                        new ArrayList< ScmFileBasicInfo >(), sortNameAtr1,
                        typeAtr1 },
                // limit > batchList.size
                { filter, positive, 0, batchList.size() + 1, batchList,
                        sortNameAtr1, typeAtr1 },
                // limit = -1
                { filter, positive, 10, -1, batchList, sortNameAtr1, typeAtr1 },
                // orderby:多个字段 逆序
                { filter, dPositive, 0, 10, batchList, sortNameAtr2, typeAtr2 },
                { filter, dPositive, 2, 20, batchList, sortNameAtr2,
                        typeAtr2 } };
    }

    @Test(dataProvider = "dataProvider", groups = { GroupTags.base })
    private void test( BSONObject filter, BSONObject orderby, long skip,
            long limit, List< ScmBatchInfo > list, String[] sortnameArr,
            boolean[] typeArr ) throws Exception {
        List< ScmBatchInfo > tmpList = new ArrayList<>();
        tmpList.addAll( list );
        ListUtils.sort( tmpList, sortnameArr, typeArr );
        int actPageSize = 0;
        long tmpSkip = skip;
        int totalNum = 0;
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            while ( tmpSkip < tmpList.size() ) {
                ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                        .listInstance( ws, filter, orderby, tmpSkip, limit );
                int count = 0;
                while ( cursor.hasNext() ) {
                    ScmBatchInfo act = cursor.getNext();
                    ScmBatchInfo exp = tmpList
                            .get( ( int ) ( tmpSkip + count ) );
                    try {
                        Assert.assertEquals( act.getName(), exp.getName() );
                        Assert.assertEquals( act.getCreateTime(),
                                exp.getCreateTime() );
                        Assert.assertEquals( act.getId(), exp.getId() );
                        Assert.assertEquals( act.getFilesCount(),
                                exp.getFilesCount() );
                        count++;
                    } catch ( AssertionError e ) {
                        throw new Exception( "filter = " + filter.toString()
                                + ",orderby = " + orderby.toString()
                                + ",skip = " + skip + ",limit = " + limit
                                + "，act = " + act.toString() + ",exp = "
                                + exp.toString(), e );
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
            int size = tmpList.size();
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
            throw new Exception( "filter = " + filter.toString() + ",orderby = "
                    + orderby.toString() + ",skip = " + skip + ",limit = "
                    + limit, e );
        }
        expSuccessTestCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( expSuccessTestCount.get() == 9 || TestScmBase.forceClear ) {
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
