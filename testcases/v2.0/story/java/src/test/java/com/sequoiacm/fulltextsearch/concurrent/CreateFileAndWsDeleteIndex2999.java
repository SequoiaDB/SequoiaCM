package com.sequoiacm.fulltextsearch.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
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
 * @Description SCM-2999:并发ws删除索引和新建文件
 * @author wuyan
 * @Date 2020.09.23
 * @version 1.00
 */

public class CreateFileAndWsDeleteIndex2999 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2999";
    private String wsName = null;
    private String matchCond = "testfile2999";
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
        // 创建文件，文件属性匹配ws创建索引的file_matcher条件
        for ( int i = 0; i < 5; i++ ) {
            String subFileName = fileName + "_" + i;
            ScmId fileId = createFile( ws, subFileName, matchCond );
            fileIds.add( fileId );
        }
    }

    // http://jira:8080/browse/SEQUOIACM-613
    @Test(enabled = false)
    private void test() throws Exception {
        String mismatchTitle = "mismatchtitle2998";
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", matchCond );

        ThreadExecutor threadExec = new ThreadExecutor();
        WsDeleteIndexThread wsDeleteIndex = new WsDeleteIndexThread();
        CreateFileThread createfileCreateIndex = new CreateFileThread(
                matchCond, matchCond );
        CreateFileThread createfileNoIndex = new CreateFileThread(
                mismatchTitle, mismatchTitle );

        threadExec.addWorker( wsDeleteIndex );
        threadExec.addWorker( createfileCreateIndex );
        threadExec.addWorker( createfileNoIndex );
        threadExec.run();

        int expNoCreateIndexCount = 7;
        int expCreateIndexCount = 0;
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.NONE,
                expNoCreateIndexCount );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                expCreateIndexCount );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );

        // 全文检索
        try {
            ScmFactory.Fulltext.simpleSeracher( ws )
                    .fileCondition( new BasicBSONObject() )
                    .scope( ScmType.ScopeType.SCOPE_ALL )
                    .notMatch( "condition" ).search();
            Assert.fail( " seracher should be failed!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.FULL_TEXT_INDEX_DISABLE,
                    e.getMessage() );
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
                ScmId fileId = createFile( ws, fileName, title );
                fileIds.add( fileId );
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
