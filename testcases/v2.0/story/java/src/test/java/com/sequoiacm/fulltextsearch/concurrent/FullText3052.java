
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
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3052 :: 并发相同工作区更新索引
 * @author fanyu
 * @Date:2020/11/18
 * @version:1.0
 */
public class FullText3052 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String fileNameBase = "file3052-";
    private String authorName1 = "auth3052A";
    private String authorName2 = "auth3052B";
    private int fileNum = 100;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.TEXT );
        prepareFile();
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
    }

    @Test
    private void test() throws Throwable {
        BSONObject fileCondition1 = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName1 ).get();
        BSONObject fileCondition2 = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName2 ).get();
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker(
                new UpdateIndex( fileCondition1, ScmFulltextMode.async ) );
        threadExec.addWorker(
                new UpdateIndex( fileCondition2, ScmFulltextMode.sync ) );
        threadExec.addWorker( new UpdateIndex( new BasicBSONObject(),
                ScmFulltextMode.async ) );
        threadExec.run();

        // 等待索引建立
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 获取工作区索引信息
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );

        // 检查结果
        if ( indexInfo.getFileMatcher().toString()
                .equals( fileCondition1.toString() ) ) {
            Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.async );
            FullTextUtils.searchAndCheckResults( ws,
                    ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                    fileCondition1 );
        } else if ( indexInfo.getFileMatcher().toString()
                .equals( fileCondition2.toString() ) ) {
            Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.sync );
            FullTextUtils.searchAndCheckResults( ws,
                    ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                    fileCondition2 );
        } else if ( indexInfo.getFileMatcher().toString()
                .equals( new BasicBSONObject().toString() ) ) {
            Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.async );
            FullTextUtils.searchAndCheckResults( ws,
                    ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                    new BasicBSONObject() );
        } else {
            throw new Exception( "error..." );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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

    private void prepareFile() throws ScmException {
        for ( int i = 0; i < fileNum / 2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setAuthor( authorName1 );
            file.setContent( filePath );
            fileIdList.add( file.save() );
        }

        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setAuthor( authorName2 );
            file.setContent( filePath );
            fileIdList.add( file.save() );
        }
    }

    private class UpdateIndex {
        private BSONObject fileCondition;
        private ScmFulltextMode mode;

        public UpdateIndex( BSONObject fileCondition, ScmFulltextMode mode ) {
            this.fileCondition = fileCondition;
            this.mode = mode;
        }

        @ExecuteOrder(step = 1)
        private void update() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.alterIndex( ws, new ScmFulltextModifiler()
                        .newFileCondition( fileCondition ).newMode( mode ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
