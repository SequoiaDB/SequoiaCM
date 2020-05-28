package com.sequoiacm.batch;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
 * @FileName SCM-1283: 创建空批次 SCM-1292: 删除空批次 SCM-1306: 空批次中获取文件列表
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class TestEmptyBatch1283 extends TestScmBase {
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
        ScmBatch emptyBatch = ScmFactory.Batch.createInstance( ws );
        emptyBatch.setName( "emptyBatch1283" );
        ScmId batchId = emptyBatch.save();

        emptyBatch = ScmFactory.Batch.getInstance( ws, batchId );
        List< ScmFile > fileList = emptyBatch.listFiles();
        Assert.assertEquals( 0, fileList.size(),
                "empty batch should has 0 file." );
        emptyBatch.delete();

        try {
            ScmFactory.Batch.getInstance( ws, batchId );
            Assert.fail( "get inexistent batch should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( ScmError.BATCH_NOT_FOUND, e.getError() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null )
            session.close();
    }
}