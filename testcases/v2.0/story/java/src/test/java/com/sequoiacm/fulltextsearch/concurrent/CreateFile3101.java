package com.sequoiacm.fulltextsearch.concurrent;

import java.util.ArrayList;

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
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-3101:ws异步模式创建索引，并发创建不同文件,分别匹配和不匹配ws的file_matcher
 * @author wuyan
 * @Date 2020.09.24
 * @version 1.00
 */

public class CreateFile3101 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3101";
    private String matchCond = "matchcreateindex3101";
    private String wsName = null;
    private ScmId batchId = null;
    private BSONObject matcher = new BasicBSONObject();

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        matcher.put( "title", matchCond );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.async ) );
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( "batch3101" );
        batchId = batch.save();

    }

    @Test
    private void test() throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();

        int createIndexNum = 30;
        ThreadExecutor threadExec = new ThreadExecutor();
        new ArrayList< CreateFileThread >();
        for ( int i = 0; i < createIndexNum; i++ ) {
            String subFileName = fileName + "_" + i;
            threadExec.addWorker(
                    new CreateFileThread( subFileName, matchCond, filePath ) );
        }

        String mismatchTitle = "mismatchtitle3101";
        CreateFileThread createFileNoIndex = new CreateFileThread(
                mismatchTitle, mismatchTitle, filePath );
        threadExec.addWorker( createFileNoIndex );
        threadExec.run();

        int expNoCreateIndexCount = 1;
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                createIndexNum );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.NONE,
                expNoCreateIndexCount );
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

    public class CreateFileThread {
        String title;
        String filePath;
        String fileName;

        public CreateFileThread( String fileName, String title,
                String filePath ) {
            this.title = title;
            this.filePath = filePath;
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName );
                file.setAuthor( fileName );
                file.setTitle( title );
                file.setContent( filePath );
                ScmId fileId = file.save();
                batch.attachFile( fileId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
