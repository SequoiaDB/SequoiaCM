package com.sequoiacm.fulltextsearch.concurrent;

import java.util.HashMap;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
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
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-3003::并发ws创建索引和更新文件，历史版本文件不匹配file_matcher
 * @author wuyan
 * @Date 2020.09.23
 * @version 1.00
 */

public class CreateFileAndWsCreateIndex3003 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String filePath = null;
    private String fileName = "file3003";
    private int fileNum = 20;
    private String wsName = null;
    private ScmId batchId = null;
    private ScmBatch batch = null;
    private HashMap< String, ScmId > fileInfoMap = new HashMap< >();

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( "batch3003" );
        batchId = batch.save();

        // 创建文件，文件属性匹配ws创建索引的file_matcher条件
        for ( int i = 0; i < fileNum; i++ ) {
            String subFileName = fileName + "_" + i;
            ScmId fileId = createFile( ws, subFileName, "title3003", batch );
            fileInfoMap.put( subFileName, fileId );
        }
    }

    @Test
    private void test() throws Exception {
        String mismatchTitle = "mismatchtitle3003";
        String matchTitle = "matchtitle3003";
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", matchTitle );

        ThreadExecutor threadExec = new ThreadExecutor();
        WsCreateIndexThread wsCreateIndex = new WsCreateIndexThread( matcher );
        for ( int i = 0; i < fileNum; ++i ) {
            // 更新属性不匹配索引条件file_matcher，不创建索引
            if ( i % 3 == 0 ) {
                ScmId fileId = fileInfoMap.get( fileName + "_" + i );
                UpdateFileThread updateFile = new UpdateFileThread( fileId,
                        mismatchTitle );
                threadExec.addWorker( updateFile );

            } else {
                // 更新属性匹配条件file_matcher，创建索引
                ScmId fileId = fileInfoMap.get( fileName + "_" + i );
                UpdateFileThread updateFile = new UpdateFileThread( fileId,
                        matchTitle );
                threadExec.addWorker( updateFile );
            }
        }
        threadExec.addWorker( wsCreateIndex );
        threadExec.run();

        for ( int i = 0; i < fileNum; ++i ) {
            if ( i % 3 == 0 ) {

                FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                        fileInfoMap.get( fileName + "_" + i ) );
            } else {
                FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                        fileInfoMap.get( fileName + "_" + i ) );
            }
        }

        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                matcher, matcher );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
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

    private class WsCreateIndexThread {
        BSONObject matcher;

        public WsCreateIndexThread( BSONObject matcher ) {
            this.matcher = matcher;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                        matcher, ScmFulltextMode.sync ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFileThread {
        ScmId fileId;
        String title;

        public UpdateFileThread( ScmId fileId, String title ) {
            this.fileId = fileId;
            this.title = title;
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
                file.setTitle( title );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private ScmId createFile( ScmWorkspace ws, String fileName, String title,
            ScmBatch batch ) throws Exception {
        filePath = TestTools.LocalFile.getFileByType( FileType.XLSX );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setTitle( title );
        file.setContent( filePath );
        ScmId fileId = file.save();
        batch.attachFile( fileId );
        return fileId;
    }
}
