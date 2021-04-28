package com.sequoiacm.fulltextsearch.concurrent;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.TestTools.LocalFile.FileType;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-3002 :: 并发更新文件
 * @author wuyan
 * @Date 2020.11.07
 * @version 1.00
 */

public class CreateFile3002 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3002";
    private String wsName = null;
    private String oldMatchCond = "测试file3002";
    private String newMatchCond = "newtestfile3002";
    private ScmId fileId = null;

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", oldMatchCond );
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.async ) );

        // 创建文件，文件属性匹配ws创建索引的file_matcher条件
        fileId = createFile( ws, fileName, oldMatchCond );
    }

    @Test
    private void test() throws Exception {
        String filePath = TestTools.LocalFile.getFileByType( FileType.XLSX );
        ThreadExecutor threadExec = new ThreadExecutor();
        CreateFileThread updatefileNoIndex = new CreateFileThread( newMatchCond,
                filePath );
        CreateFileThread updatefileCreateIndex = new CreateFileThread(
                oldMatchCond, filePath );
        threadExec.addWorker( updatefileNoIndex );
        threadExec.addWorker( updatefileCreateIndex );
        threadExec.run();

        if ( updatefileNoIndex.getRetCode() == 0 ) {
            Assert.assertEquals( updatefileCreateIndex.getRetCode(),
                    ScmError.FILE_VERSION_MISMATCHING.getErrorCode(),
                    "update file(" + fileId + ") with createIndex fail:"
                            + updatefileCreateIndex.getThrowable()
                                    .getMessage() );
            FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                    fileId );
        } else {
            Assert.assertEquals( updatefileCreateIndex.getRetCode(), 0 );
            Assert.assertEquals( updatefileNoIndex.getRetCode(),
                    ScmError.FILE_VERSION_MISMATCHING.getErrorCode(),
                    "update file(" + fileId + ") fail:"
                            + updatefileNoIndex.getThrowable().getMessage() );
            FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                    fileId );
            BSONObject matcher = new BasicBSONObject();
            matcher.put( "title", newMatchCond );
            FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                    matcher, matcher );
        }
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

    private class CreateFileThread extends ResultStore {
        String title;
        String filePath;

        public CreateFileThread( String title, String filePath ) {
            this.title = title;
            this.filePath = filePath;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.updateContent( filePath );
                file.setMimeType( MimeType.XLSX );
                file.setTitle( title );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private ScmId createFile( ScmWorkspace ws, String fileName, String title )
            throws Exception {
        String filePath = TestTools.LocalFile.getFileByType( FileType.XLSX );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setTitle( title );
        file.setContent( filePath );
        file.setMimeType( MimeType.XLSX );
        ScmId fileId = file.save();
        return fileId;
    }
}
