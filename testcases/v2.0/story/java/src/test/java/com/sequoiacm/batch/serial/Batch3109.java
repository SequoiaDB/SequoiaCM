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
 * @Description SCM-3109:指定相同的批次id，创建批次
 * @author fanyu
 * @version 1.00
 * @Date 2020/10/13
 */
public class Batch3109 extends TestScmBase {
    private String wsName = "ws3109";
    private String batchName = "batch3109";
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
        // 指定id创建批次
        String batchIdA = "NONO20201013";
        ScmBatch batchA = ScmFactory.Batch.createInstance( ws, batchIdA );
        batchA.setName( batchName );
        batchA.save();
        ScmBatch getBatch = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchIdA, false ) );
        Assert.assertEquals( getBatch.listFiles().size(), 0 );

        // 重复指定id创建批次
        String batchIdB = "NONO20201013";
        ScmBatch batchB = ScmFactory.Batch.createInstance( ws, batchIdB );
        batchB.setName( batchName );
        try {
            batchB.save();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BATCH_EXIST ) {
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
