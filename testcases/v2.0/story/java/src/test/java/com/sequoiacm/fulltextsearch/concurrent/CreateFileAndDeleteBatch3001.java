package com.sequoiacm.fulltextsearch.concurrent;

import org.bson.BSONObject;
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
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-3001:并发更新文件和删除批次
 * @author wuyan
 * @Date 2020.09.23
 * @version 1.00
 */

public class CreateFileAndDeleteBatch3001 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3001";
    private String matchCond = "testfile3001";
    private String wsName = null;
    private ScmId batchId = null;
    private ScmId fileId = null;
    private ScmBatch batch = null;

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( "batch3001" );
        batchId = batch.save();

        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", matchCond );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

    }

    @Test
    private void test() throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();
        fileId = ScmFileUtils.create( ws, fileName, filePath );

        ThreadExecutor threadExec = new ThreadExecutor();
        DeleteBatchThread deleteBatchIndex = new DeleteBatchThread();
        UpdateFileThread updateFile = new UpdateFileThread( filePath );
        threadExec.addWorker( deleteBatchIndex );
        threadExec.addWorker( updateFile );
        threadExec.run();

        if ( deleteBatchIndex.getRetCode() == 0 ) {
            if ( updateFile.getRetCode() != 0 ) {
                Assert.assertEquals( updateFile.getRetCode(),
                        ScmError.FILE_EXIST.getErrorCode(),
                        "update file(" + fileId + ") with createIndex fail:"
                                + updateFile.getThrowable().getMessage() );
            } else {
                Assert.assertEquals( updateFile.getRetCode(), 0 );
            }

        } else {
            Assert.fail( "delete batch not should be failed!" );
        }

        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 0 );
        // 全文检索
        ScmCursor< ScmFulltextSearchResult > result1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).notMatch( "condition" )
                .search();

        Assert.assertFalse( result1.hasNext() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Fulltext.dropIndex( ws );
                FullTextUtils.waitWorkSpaceIndexStatus( ws,
                        ScmFulltextStatus.NONE );
            }
        } finally {
            if ( wsName != null ) {
                WsPool.release( wsName );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class DeleteBatchThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Batch.deleteInstance( ws, batchId );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFileThread extends ResultStore {
        String filePath;

        public UpdateFileThread( String filePath ) {
            this.filePath = filePath;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.updateContent( filePath );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
