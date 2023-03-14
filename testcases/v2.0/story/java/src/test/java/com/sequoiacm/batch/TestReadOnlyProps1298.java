package com.sequoiacm.batch;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @FileName SCM-1298: 批次只读属性测试
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

// driver can't create new user yet.
// so, in this automatic testcase, just use one user.
// it should be improved in future.
public class TestReadOnlyProps1298 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );
        // TODO: should create new user
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( "batch1298" );
        batchId = batch.save();
        long sleepTimeBeforeUpdate = 1000;
        Thread.sleep( sleepTimeBeforeUpdate );

        // TODO: should update by new user
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        batch.setName( "new_name1298" );

        batch = ScmFactory.Batch.getInstance( ws, batchId );
        batch.setName( "new_name1298" );
        Assert.assertEquals( batch.getCreateUser(), TestScmBase.scmUserName );
        Assert.assertEquals( batch.getUpdateUser(), TestScmBase.scmUserName );
        long timeDiff = batch.getUpdateTime().getTime()
                - batch.getCreateTime().getTime();
        if ( timeDiff < sleepTimeBeforeUpdate ) {
            Assert.fail( "update time is wrong" );
        }
        Assert.assertEquals( batch.getWorkspaceName(), ws.getName() );
        Assert.assertEquals( batch.getId().get(), batchId.get() );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmFactory.Batch.deleteInstance( ws, batchId );
        if ( session != null )
            session.close();
    }
}