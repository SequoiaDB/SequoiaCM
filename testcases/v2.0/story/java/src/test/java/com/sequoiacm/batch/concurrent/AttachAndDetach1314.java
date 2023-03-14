package com.sequoiacm.batch.concurrent;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @FileName SCM-1314: 并发添加和解除文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class AttachAndDetach1314 extends TestScmBase {
    private final String batchName = "batch1314";
    private final int fileNum = 20;
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>( fileNum );
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );

        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "file1314_" + i );
            file.setTitle( batchName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }

        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        AttachThread attachThrd = new AttachThread();
        DetachThread detachThrd = new DetachThread();
        attachThrd.start();
        detachThrd.start();
        Assert.assertTrue( attachThrd.isSuccess(), attachThrd.getErrorMsg() );
        Assert.assertTrue( detachThrd.isSuccess(), detachThrd.getErrorMsg() );

        // TODO: when file.getBatchId() finish. check if the map between
        // batch and file is ok.
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
            try {
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                for ( ScmId fileId : fileIdList ) {
                    batch.detachFile( fileId );
                }
            } catch ( ScmException e ) {
                // TODO:DETACH不存在的文件错误码不对
                // if (e.getError() != ScmError.FILE_NOT_IN_BATCH) {
                // throw e;
                // }
            }
        }
    }

    private class AttachThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
            for ( ScmId fileId : fileIdList ) {
                batch.attachFile( fileId );
            }
        }
    }
}