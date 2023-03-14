package com.sequoiacm.batch.serial;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3108:指定批次的id不符合批次id规则，创建/查询/删除批次
 * @author fanyu
 * @version 1.00
 * @Date 2020/10/13
 */
public class Batch3108 extends TestScmBase {
    private String wsName = "ws3108";
    private String batchName = "batch3108";
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type,比如MONTH,设置batch_id_time_regexp、batch_id_time_parttern
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.MONTH, "[0-9].*", "yyyyMMdd", false );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    private void test() throws Exception {
        // 批次id中的时间格式不符合batch_id_time_parttern
        String batchIdA = "NONO";
        // 创建批次
        ScmBatch batchA = ScmFactory.Batch.createInstance( ws, batchIdA );
        batchA.setName( batchName );
        try {
            batchA.save();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ID ) {
                throw e;
            }
        }
        // 查询批次
        try {
            ScmFactory.Batch.getInstance( ws, new ScmId( batchIdA, false ) );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BATCH_NOT_FOUND ) {
                throw e;
            }
        }

        // 删除批次
        try {
            ScmFactory.Batch.deleteInstance( ws, new ScmId( batchIdA, false ) );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BATCH_NOT_FOUND ) {
                throw e;
            }
        }

        // batch_id_time_regexp匹配到的信息不正确 创建批次
        String batchIdB = "NONO2020";
        // 创建批次
        ScmBatch batchB = ScmFactory.Batch.createInstance( ws, batchIdB );
        batchB.setName( batchName );
        try {
            batchB.save();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ID ) {
                throw e;
            }
        }
        // 获取批次
        try {
            ScmFactory.Batch.getInstance( ws, new ScmId( batchIdB, false ) );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BATCH_NOT_FOUND ) {
                throw e;
            }
        }
        // 删除批次
        try {
            ScmFactory.Batch.deleteInstance( ws, new ScmId( batchIdB, false ) );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BATCH_NOT_FOUND ) {
                throw e;
            }
        }

        // 时间会自动向前进位
        String batchIdC = "NO20201313";
        ScmBatch batchC = ScmFactory.Batch.createInstance( ws, batchIdC );
        batchC.setName( batchName );
        batchC.save();
        Assert.assertEquals( batchC.listFiles().size(), 0 );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null )
            session.close();
    }
}
