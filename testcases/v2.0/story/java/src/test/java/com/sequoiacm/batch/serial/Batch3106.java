package com.sequoiacm.batch.serial;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3106:指定batch_sharding_type，覆盖：YEAR|MONTH|QUARTER，创建/删除批次
 * @author fanyu
 * @version 1.00
 * @Date 2020/10/13
 */
public class Batch3106 extends TestScmBase {
    private String wsName = "ws3106";
    private String batchName = "batch3106";
    private String fileNameBase = "file3106_";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String batchIdA = "20201013";
    private String batchIdB = "20190101";
    private String batchIdC = "19700101";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @DataProvider(name = "shardingTypeProvider")
    public Object[][] generateSeekSize() {
        return new Object[][] { { ScmShardingType.YEAR },
                { ScmShardingType.QUARTER }, { ScmShardingType.MONTH } };
    }

    @Test(dataProvider = "shardingTypeProvider", groups = { GroupTags.base })
    private void test( ScmShardingType type ) throws Exception {
        // 指定batch_sharding_type为YEAR,设置batch_id_time_regexp、batch_id_time_parttern
        int times = 0;
        do {
            try {
                ws = ScmWorkspaceUtil.createWS( session, wsName,
                        ScmInfo.getSiteNum(), type, ".*", "yyyyMMdd", false );
                break;
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.CONFIG_SERVER_ERROR ) {
                    throw e;
                } else {
                    times++;
                    Thread.sleep( 1000 );
                }
                if ( times >= 120 ) {
                    throw new Exception(
                            "createWS time out, final Exception message is :"
                                    + e.getMessage() );
                }
            }
        } while ( true );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        // 创建文件
        List< ScmId > fileIdList = new ArrayList<>();
        for ( int i = 0; i < 3; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            fileIdList.add( file.save() );
        }

        // 指定id创建批次
        ScmBatch batchA = ScmFactory.Batch.createInstance( ws, batchIdA );
        batchA.setName( batchName );
        batchA.save();
        batchA.attachFile( fileIdList.get( 0 ) );

        ScmBatch batchB = ScmFactory.Batch.createInstance( ws, batchIdB );
        batchB.setName( batchName );
        batchB.save();
        batchB.attachFile( fileIdList.get( 1 ) );

        ScmBatch batchC = ScmFactory.Batch.createInstance( ws, batchIdC );
        batchC.setName( batchName );
        batchC.save();
        batchC.attachFile( fileIdList.get( 2 ) );

        // 获取批次检查结果
        long count1 = ScmFactory.Batch.countInstance( ws, ScmQueryBuilder
                .start( ScmAttributeName.Batch.NAME ).is( batchName ).get() );
        Assert.assertEquals( count1, 3 );
        // 删除批次
        ScmFactory.Batch.deleteInstance( ws, new ScmId( batchIdA, false ) );
        ScmFactory.Batch.deleteInstance( ws, new ScmId( batchIdB, false ) );
        ScmFactory.Batch.deleteInstance( ws, new ScmId( batchIdC, false ) );

        // 检查结果
        long count2 = ScmFactory.Batch.countInstance( ws, ScmQueryBuilder
                .start( ScmAttributeName.Batch.NAME ).is( batchName ).get() );
        Assert.assertEquals( count2, 0 );

        // 删除工作区
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( session != null )
            session.close();
    }
}
