package com.sequoiacm.batch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2590:通过条件查询文件夹数量
 * @author fanyu
 * @Date:2019年09月03日
 * @version:1.0
 */
public class CountBatch2596 extends TestScmBase {
    private AtomicInteger successCount = new AtomicInteger( 0 );
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private int batchNum = 100;
    private List< ScmId > batchIdList = new ArrayList<>();
    private List< String > batchNames = new ArrayList<>();
    private String batchNamePrefix = "batch2596";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // prepare batch
        for ( int i = 0; i < batchNum; i++ ) {
            String batchName = batchNamePrefix + "-" + i;
            ScmBatch scmBatch = ScmFactory.Batch.createInstance( ws );
            scmBatch.setName( batchName );
            batchIdList.add( scmBatch.save() );
            batchNames.add( batchName );
        }
    }

    @Test
    private void testAll() throws Exception {
        BSONObject filter = ScmQueryBuilder.start().get();
        long count = ScmFactory.Batch.countInstance( ws, filter );
        Assert.assertTrue( count >= batchNum, filter.toString() );
        successCount.getAndIncrement();
    }

    @Test
    private void testZero() throws Exception {
        BSONObject filter = ScmQueryBuilder
                .start( ScmAttributeName.Directory.NAME )
                .is( batchNamePrefix + "-inexistence" ).get();
        long count = ScmFactory.Batch.countInstance( ws, filter );
        Assert.assertEquals( count, 0, filter.toString() );
        successCount.getAndIncrement();
    }

    @Test
    private void testPart() throws Exception {
        BSONObject filter = ScmQueryBuilder.start( ScmAttributeName.Batch.NAME )
                .in( batchNames ).get();
        long count = ScmFactory.Batch.countInstance( ws, filter );
        Assert.assertEquals( count, batchNum, filter.toString() );

        BSONObject filter1 = ScmQueryBuilder
                .start( ScmAttributeName.Directory.NAME )
                .in( batchNames.get( 0 ) ).get();
        long count1 = ScmFactory.Batch.countInstance( ws, filter1 );
        Assert.assertEquals( count1, 1, filter.toString() );
        successCount.getAndIncrement();
    }

    @Test
    private void testInvalidWs() throws Exception {
        try {
            ScmFactory.Batch.countInstance( null, new BasicBSONObject() );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        successCount.getAndIncrement();
    }

    @Test
    private void testInvalidBSON() throws Exception {
        try {
            ScmFactory.Batch.countInstance( ws, null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        successCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( successCount.get() == 5 || TestScmBase.forceClear ) {
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
