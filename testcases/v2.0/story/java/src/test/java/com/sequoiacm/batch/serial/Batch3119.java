package com.sequoiacm.batch.serial;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3119:并发创建多个批次
 * @author fanyu
 * @Date:2020/10/15
 * @version:1.0
 */

public class Batch3119 extends TestScmBase {
    private SiteWrapper site = null;
    private String wsName = "ws3119";
    private String batchName = "batch3119";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< String > batchIdList = new ArrayList<>();
    private int batchNum = 30;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type为NONE,设置batch_id_time_regexp、batch_id_time_parttern
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.YEAR, ".*", "yyyy", true );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < batchNum; i++ ) {
            String batchId = "" + ( 2020 + i );
            batchIdList.add( batchId );
            threadExec.addWorker( new CreateBatch( batchId ) );
        }
        threadExec.run();

        // 获取批次信息，检查结果
        for ( String batchId : batchIdList ) {
            ScmBatch batch = ScmFactory.Batch.getInstance( ws,
                    new ScmId( batchId, false ) );
            Assert.assertEquals( batch.getName(), batchName, batchId );
            Assert.assertEquals( batch.listFiles().size(), 0, batchId );
            Assert.assertEquals( batch.getId().get(), batchId, batchId );
        }
    }

    @AfterClass
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null )
            session.close();
    }

    private class CreateBatch {
        private String batchId;

        public CreateBatch( String batchId ) {
            this.batchId = batchId;
        }

        @ExecuteOrder(step = 1)
        private void create() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmBatch batch = ScmFactory.Batch.createInstance( ws, batchId );
                batch.setName( batchName );
                batch.save();
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
