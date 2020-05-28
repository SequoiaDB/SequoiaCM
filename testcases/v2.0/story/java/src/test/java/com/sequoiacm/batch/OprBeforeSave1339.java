package com.sequoiacm.batch;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-1339: 保存批次前进行批次操作
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class OprBeforeSave1339 extends TestScmBase {
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
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( "batch1339" );
        // batch.save();
        ScmId fileId = new ScmId( "ffffffffffffffffffffffff" );
        try {
            batch.attachFile( fileId );
            Assert.fail( "attach file should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.OPERATION_UNSUPPORTED );
        }

        try {
            batch.detachFile( fileId );
            Assert.fail( "detach file should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.OPERATION_UNSUPPORTED );
        }

        // TODO: fail for SEQUOIACM-247
        try {
            batch.listFiles();
            Assert.fail( "list files should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.OPERATION_UNSUPPORTED );
        }

        try {
            batch.delete();
            Assert.fail( "detach file should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.OPERATION_UNSUPPORTED );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( session != null )
            session.close();
    }
}