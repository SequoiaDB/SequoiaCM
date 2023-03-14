package com.sequoiacm.batch.serial;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3102:批次不分区，未设置批次id规则，指定和不指定批次id，创建/删除批次
 * @author fanyu
 * @version 1.00
 * @Date 2020/10/13
 */
public class Batch3102 extends TestScmBase {
    private String wsName = "ws3102";
    private String batchName = "batch3102";
    private String fileName = "file3102";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private ScmId batchIdA = null;
    private String batchIdB = "NO3102";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type为NONE,不设置batch_id_time_regexp、batch_id_time_parttern
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.NONE, null, null, false );
        Assert.assertEquals( ws.getBatchShardingType(), ScmShardingType.NONE );
        Assert.assertEquals( ws.getBatchIdTimePattern(), null );
        Assert.assertEquals( ws.getBatchIdTimeRegex(), null );

        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        fileId = file.save();
    }

    @Test
    private void test() throws Exception {
        // 不指定id创建批次
        ScmBatch batchA = ScmFactory.Batch.createInstance( ws );
        batchA.setName( batchName );
        batchIdA = batchA.save();

        // 指定id创建批次
        ScmBatch batchB = ScmFactory.Batch.createInstance( ws, batchIdB );
        batchB.setName( batchName );
        batchB.save();
        batchB.attachFile( fileId );

        // 获取批次检查结果
        checkResults();

        // 删除批次
        ScmFactory.Batch.deleteInstance( ws, new ScmId( batchIdB, false ) );
        ScmFactory.Batch.deleteInstance( ws, batchIdA );

        // 检查结果
        long count = ScmFactory.Batch.countInstance( ws, ScmQueryBuilder
                .start( ScmAttributeName.Batch.NAME ).is( batchName ).get() );
        Assert.assertEquals( count, 0 );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null )
            session.close();
    }

    private void checkResults() throws ScmException {
        // 获取批次检查结果
        ScmBatch getBatchA = ScmFactory.Batch.getInstance( ws, batchIdA );
        Assert.assertEquals( getBatchA.getName(), batchName );
        Assert.assertEquals( getBatchA.getId(), batchIdA );
        List< ScmFile > files = getBatchA.listFiles();
        Assert.assertEquals( files.size(), 0 );

        ScmBatch getBatchB = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchIdB, false ) );
        Assert.assertEquals( getBatchB.getName(), batchName );
        Assert.assertEquals( getBatchB.getId(), new ScmId( batchIdB, false ) );
        files = getBatchB.listFiles();
        Assert.assertEquals( files.size(), 1 );
        Assert.assertEquals( files.get( 0 ).getFileName(), fileName );
    }
}
