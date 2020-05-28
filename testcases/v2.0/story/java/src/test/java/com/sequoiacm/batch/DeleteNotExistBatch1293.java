package com.sequoiacm.batch;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-1293: 删除不存在的批次
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class DeleteNotExistBatch1293 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmId inexistentId = new ScmId( "ffffffffffffffffffffffff" );
        try {
            ScmFactory.Batch.deleteInstance( ws, inexistentId );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BATCH_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( session != null )
            session.close();
    }
}