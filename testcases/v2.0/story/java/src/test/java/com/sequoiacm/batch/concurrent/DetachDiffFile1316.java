package com.sequoiacm.batch.concurrent;

import java.util.List;
import java.util.Stack;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1316: 并发解除不同文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class DetachDiffFile1316 extends TestScmBase {
    private final String batchName = "batch1316";
    private final int threadNum = 3;
    private final int fileNumPerThrd = 10;
    private final int fileNum = threadNum * fileNumPerThrd;
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private Stack< ScmId > fileIdStack = new Stack<>();
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );

        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "file1316_" + i );
            file.setTitle( batchName );
            ScmId fileId = file.save();
            fileIdStack.push( fileId );
        }

        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();
        for ( ScmId fileId : fileIdStack )
            batch.attachFile( fileId );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        DetachThread detachThrd = new DetachThread();
        detachThrd.start( threadNum );
        Assert.assertTrue( detachThrd.isSuccess(), detachThrd.getErrorMsg() );

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
                ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                        .listInstance( ws, ScmType.ScopeType.SCOPE_CURRENT,
                                new BasicBSONObject( "title", batchName ) );
                while ( cursor.hasNext() ) {
                    ScmFileBasicInfo info = cursor.getNext();
                    ScmFactory.File.deleteInstance( ws, info.getFileId(),
                            true );
                }
                cursor.close();
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }

    private class DetachThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
            for ( int i = 0; i < fileNumPerThrd; ++i ) {
                ScmId fileId = fileIdStack.pop();
                batch.detachFile( fileId );
            }
        }
    }
}