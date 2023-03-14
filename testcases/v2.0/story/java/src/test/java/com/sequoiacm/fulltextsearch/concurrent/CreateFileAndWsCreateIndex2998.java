package com.sequoiacm.fulltextsearch.concurrent;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-2998:并发ws创建索引和新建文件
 * @author wuyan
 * @Date 2020.09.23
 * @version 1.00
 */

public class CreateFileAndWsCreateIndex2998 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2998";
    private String wsName = null;
    private ScmId batchId = null;
    private ScmBatch batch = null;
    private MultiValueMap< String, ScmId > fileInfoMap = new LinkedMultiValueMap< String, ScmId >();

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( "batch2998" );
        batchId = batch.save();

        // 创建文件，文件属性匹配ws创建索引的file_matcher条件
        for ( int i = 0; i < 5; i++ ) {
            String subFileName = fileName + "_" + i;
            createFile( ws, subFileName, "matchtitle2998", batch );
        }
    }

    @Test
    private void test() throws Exception {
        String mismatchTitle = "mismatchtitle2998";
        String matchTitle = "matchtitle2998";
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", matchTitle );

        ThreadExecutor threadExec = new ThreadExecutor(300000);
        WsCreateIndexThread wsCreateIndex = new WsCreateIndexThread( matcher );
        CreateFileThread createfileCreateIndex = new CreateFileThread(
                matchTitle, matchTitle );
        CreateFileThread createfileNoIndex = new CreateFileThread(
                mismatchTitle, mismatchTitle );
        threadExec.addWorker( wsCreateIndex );
        threadExec.addWorker( createfileCreateIndex );
        threadExec.addWorker( createfileNoIndex );

        threadExec.run();

        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                fileInfoMap.get( matchTitle ).get( 0 ) );
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                fileInfoMap.get( mismatchTitle ).get( 0 ) );
        int expNoCreateIndexCount = 20;
        int expCreateIndexCount = 25;
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.NONE,
                expNoCreateIndexCount );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                expCreateIndexCount );
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
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
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

    private class CreateFileThread {
        String fileName;
        String title;

        public CreateFileThread( String fileName, String title ) {
            this.fileName = fileName;
            this.title = title;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                for ( int i = 0; i < 20; i++ ) {
                    String name = fileName + "_" + i;
                    ScmId fileId = createFile( ws, name, title, batch );
                    fileInfoMap.add( title, fileId );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private ScmId createFile( ScmWorkspace ws, String fileName, String title,
            ScmBatch batch ) throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();
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
