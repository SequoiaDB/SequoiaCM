package com.sequoiacm.batch;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Description: SCM-3132:createInstance()参数校验
 * @author fanyu
 * @Date:2020/10/16
 * @version:1.0
 */

public class Param_CreateInstance3132 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws3132";
    private ScmWorkspace ws = null;
    private String batchName = "batch3132";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type为YEAR,设置batch_id_time_regexp、batch_id_time_parttern
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.YEAR, "[0-9]\\d*\\.?\\d*.?\\d*", "yyyy.MM.dd",
                false );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    private void test1() throws Exception {
        // 英文+时间其他用例有覆盖到，中文+ 时间
        String batchId = "！~@#￥&2020.10.16测试";
        ScmBatch batch = ScmFactory.Batch.createInstance( ws, batchId );
        batch.setName( batchName );
        batch.save();
        // 检查结果
        ScmBatch getBatch = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchId, false ) );
        Assert.assertEquals( getBatch.getId().get(), batchId );
    }

    @Test
    private void test2() throws Exception {
        try {
            ScmFactory.Batch.createInstance( null, "2020" );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmBatch batch = ScmFactory.Batch.createInstance( ws, null );
            batch.setName( "batch3132" );
            batch.save();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null )
            session.close();
    }
}
