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
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-2996:并发创建相同文件,其中文件属性分别设置为匹配和不匹配ws的file_matcher
 * @author wuyan
 * @Date 2020.09.22
 * @version 1.00
 */

public class CreateFile2996 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file2996";
    private String wsName = null;
    private BSONObject matcher = new BasicBSONObject();

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        matcher.put( "title", "matchtitle2996" );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );

    }

    @Test
    private void test() throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();
        String title1 = "mismatchtitle2996";
        String title2 = "matchtitle2996";
        ThreadExecutor threadExec = new ThreadExecutor();
        CreateFileThread createfileNoIndex = new CreateFileThread( title1,
                filePath );
        CreateFileThread createfileCreateIndex = new CreateFileThread( title2,
                filePath );
        threadExec.addWorker( createfileNoIndex );
        threadExec.addWorker( createfileCreateIndex );
        threadExec.run();

        if ( createfileNoIndex.getRetCode() == 0 ) {
            Assert.assertEquals( createfileCreateIndex.getRetCode(),
                    ScmError.FILE_EXIST.getErrorCode(),
                    "create file(" + fileId + ") with createIndex fail:"
                            + createfileCreateIndex.getThrowable()
                                    .getMessage() );
            FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                    fileId );

        } else {
            Assert.assertEquals( createfileCreateIndex.getRetCode(), 0 );
            Assert.assertEquals( createfileNoIndex.getRetCode(),
                    ScmError.FILE_EXIST.getErrorCode(),
                    "create file(" + fileId + ") fail:"
                            + createfileNoIndex.getThrowable().getMessage() );
            FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                    fileId );
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

    public class CreateFileThread extends ResultStore {
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
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName );
                file.setAuthor( fileName );
                file.setTitle( title );
                file.setContent( filePath );
                fileId = file.save();
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
