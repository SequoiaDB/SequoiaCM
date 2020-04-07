package com.sequoiacm.batch.concurrent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1312: 并发解除同一个文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class DetachSameFile1312 extends TestScmBase {
    private final String batchName = "batch1311";
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace
                .getWorkspace( ScmInfo.getWs().getName(), session );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( "file1311" );
        file.setTitle( batchName );
        fileId = file.save();

        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();
        batch.attachFile( fileId );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        DetachThread detachThrd = new DetachThread();
        detachThrd.start( 5 );
        Assert.assertTrue( detachThrd.isSuccess(), detachThrd.getErrorMsg() );
        Assert.assertEquals( detachThrd.getSuccessTimes(), 1 );

        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        List< ScmFile > files = batch.listFiles();
        Assert.assertEquals( files.size(), 0 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }

    private class DetachThread extends TestThreadBase {
        private AtomicInteger successTimes = new AtomicInteger( 0 );

        @Override
        public void exec() throws Exception {
            try {
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                batch.detachFile( fileId );
                successTimes.getAndIncrement();
            } catch ( ScmException e ) {
                //TODO:错误码不对
//				if (e.getError() != ScmError.FILE_NOT_IN_BATCH) {
//					throw e;
//				}
            }
        }

        public int getSuccessTimes() {
            return successTimes.get();
        }
    }
}