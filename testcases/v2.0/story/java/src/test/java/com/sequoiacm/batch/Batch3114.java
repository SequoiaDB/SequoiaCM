package com.sequoiacm.batch;

import java.text.SimpleDateFormat;
import java.util.UUID;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3114:批次有分区，列取批次
 * @author fanyu
 * @version 1.00
 * @Date 2020/10/13
 */
public class Batch3114 extends TestScmBase {
    private String wsName = "ws3114";
    private String batchName = "batch3114";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private int batchNum = 10;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type为YEAR,设置batch_id_time_regexp、batch_id_time_parttern
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.YEAR, "(^\\d{4})(?=.*)", "yyyy", false );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    private void test() throws Exception {
        // 创建批次
        for ( int i = 0; i < batchNum; i++ ) {
            String batchId = ( 2020 - i % 5 ) + UUID.randomUUID().toString();
            ScmBatch batch = ScmFactory.Batch.createInstance( ws, batchId );
            batch.setName( batchName );
            batch.save();
        }

        // 列取批次
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.listInstance( ws,
                new BasicBSONObject() );
        SimpleDateFormat format = new SimpleDateFormat( "yyyy" );
        int i = 0;
        while ( cursor.hasNext() ) {
            i++;
            ScmBatchInfo info = cursor.getNext();
            Assert.assertEquals( info.getName(), batchName );
            Assert.assertEquals( info.getFilesCount(), 0 );
            Assert.assertTrue( format.parse( "2016" ).getTime() <= info
                    .getCreateTime().getTime() );
            Assert.assertTrue( info.getCreateTime().getTime() <= format
                    .parse( "2020" ).getTime() );
        }
        Assert.assertEquals( i, batchNum );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null )
            session.close();
    }
}
