package com.sequoiacm.batch;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3105:批次有分区，未设置批次id规则，指定和不指定批次id，创建批次
 * @author fanyu
 * @version 1.00
 * @Date 2020/10/13
 */
public class Batch3105 extends TestScmBase {
    private String wsName = "ws3105";
    private String batchName = "batch3105";
    private String fileName = "file3105";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String batchIdA = "20201013";
    private ScmId batchIdB = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type,比如MONTH,未设置batch_id_time_regexp、batch_id_time_parttern
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.YEAR, null, null, false );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        fileId = file.save();
    }

    @Test
    private void test() throws Exception {
        // 指定id创建批次
        ScmBatch batchA = ScmFactory.Batch.createInstance( ws, batchIdA );
        batchA.setName( batchName );
        try {
            batchA.save();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // 不指定id创建批次
        ScmBatch batchB = ScmFactory.Batch.createInstance( ws );
        batchB.setName( batchName );
        batchIdB = batchB.save();
        batchB.attachFile( fileId );

        // 获取批次，检查结果
        checkResults();

        // 删除批次
        ScmFactory.Batch.deleteInstance( ws, batchIdB );

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
        ScmBatch getBatchB = ScmFactory.Batch.getInstance( ws, batchIdB );
        Assert.assertEquals( getBatchB.getName(), batchName );
        Assert.assertEquals( getBatchB.getId(), batchIdB );
        List< ScmFile > files = getBatchB.listFiles();
        Assert.assertEquals( files.size(), 1 );
        Assert.assertEquals( files.get( 0 ).getFileName(), fileName );
    }
}
