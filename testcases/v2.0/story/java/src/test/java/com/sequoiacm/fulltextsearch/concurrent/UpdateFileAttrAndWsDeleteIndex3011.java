package com.sequoiacm.fulltextsearch.concurrent;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-3011 :: 并发ws删除索引和更新文件属性
 * @author wuyan
 * @Date 2020.09.23
 * @version 1.00
 */

public class UpdateFileAttrAndWsDeleteIndex3011 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3011";
    private int fileNum = 20;
    private String wsName = null;
    private String matchCond = "测试newtestfile3011";
    private List< ScmId > fileIds = new ArrayList< >();

    @BeforeClass(enabled = false)
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", matchCond );
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );

        // 创建文件，只有10个文件匹配ws创建索引的file_matcher条件
        for ( int i = 0; i < fileNum; i++ ) {
            String subFileName = fileName + "_" + i;
            if ( i < 10 ) {
                ScmId fileId = createFile( ws, subFileName, matchCond );
                fileIds.add( fileId );
            } else {
                ScmId fileId = createFile( ws, subFileName, "noMatchIndex" );
                fileIds.add( fileId );
            }
        }
    }

    // http://jira:8080/browse/SEQUOIACM-613
    @Test(enabled = false)
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new WsDeleteIndexThread() );
        for ( int i = 0; i < fileNum; i++ ) {
            // 10个文件，更新后属性不匹配file_matcher条件
            if ( i < 10 ) {
                ScmId fileId = fileIds.get( i );
                UpdateFileAttrThread updateFileAttr = new UpdateFileAttrThread(
                        fileId, "noMatchIndex" );
                threadExec.addWorker( updateFileAttr );
                // 10个文件，更新后属性匹配file_matcher条件
            } else {
                ScmId fileId = fileIds.get( i );
                UpdateFileAttrThread updateFileAttr = new UpdateFileAttrThread(
                        fileId, matchCond );
                threadExec.addWorker( updateFileAttr );
            }
        }

        threadExec.run();

        // 检查ws索引状态和文件索引状态
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );
        for ( int i = 0; i < fileNum; i++ ) {
            FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                    fileIds.get( i ) );
        }
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.NONE,
                fileNum );
        // 检索文件
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", matchCond );
        try {
            FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                    matcher, matcher );
            Assert.fail( "search must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FULL_TEXT_INDEX_DISABLE != e.getError() ) {
                Assert.fail( "expErrorCode:-904  actError:" + e.getError()
                        + e.getMessage() );
            }
        }

        runSuccess = true;
    }

    @AfterClass(enabled = false)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileIds.size(); i++ ) {
                    ScmId fileId = fileIds.get( i );
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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

    private class WsDeleteIndexThread {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.dropIndex( ws );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFileAttrThread {
        ScmId fileId;
        String title;

        public UpdateFileAttrThread( ScmId fileId, String title ) {
            this.fileId = fileId;
            this.title = title;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                System.out.println( "--fileID=" + fileId + "--title=" + title );
                file.setTitle( title );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private ScmId createFile( ScmWorkspace ws, String fileName, String title )
            throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setTitle( title );
        file.setContent( filePath );
        ScmId fileId = file.save();
        return fileId;
    }

}
