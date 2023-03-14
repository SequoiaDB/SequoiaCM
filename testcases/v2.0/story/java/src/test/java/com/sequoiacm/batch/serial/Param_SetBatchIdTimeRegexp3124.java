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
 * @Description: SCM-3124:setBatchIdTimeRegexp参数校验
 * @author fanyu
 * @Date:2020/10/16
 * @version:1.0
 */

public class Param_SetBatchIdTimeRegexp3124 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws3124";
    private String batchName = "batch3124";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
    }

    @Test
    private void test1() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type为YEAR,设置batch_id_time_regexp、batch_id_time_parttern
        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, wsName,
                ScmInfo.getSiteNum(), ScmShardingType.YEAR,
                "[^\\u4E00-\\u9FA5]+", "yyyy.MM.dd", false );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        // 检查工作区属性
        Assert.assertEquals( ws.getBatchIdTimePattern(), "yyyy.MM.dd" );
        Assert.assertEquals( ws.getBatchIdTimeRegex(), "[^\\u4E00-\\u9FA5]+" );
        Assert.assertEquals( ws.getBatchShardingType(), ScmShardingType.YEAR );
        Assert.assertEquals( ws.isBatchFileNameUnique(), false );

        // 英文+时间其他用例有覆盖到，中文+ 时间
        String batchId = "批次2020.10.16";
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
        // 指定batch_sharding_type为YEAR,设置batch_id_time_regexp、batch_id_time_parttern
        try {
            ScmWorkspaceUtil.createWS( session, wsName + "_notexist",
                    ScmInfo.getSiteNum(), ScmShardingType.YEAR, null,
                    "yyyy.MM.dd", false );
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
