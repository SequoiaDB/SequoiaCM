
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
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3050 :: 并发工作区创建索引
 * @author fanyu
 * @Date:2020/11/18
 * @version:1.0
 */
public class FullText3050 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private String fileNameBase = "file3050-";
    private String authorName1 = "auth3050A";
    private String authorName2 = "auth3050B";
    private int fileNum = 20;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.TEXT );
        prepareFile();
    }

    @Test
    private void test() throws Throwable {
        BSONObject fileCondition1 = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName1 ).get();
        BSONObject fileCondition2 = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName2 ).get();
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker(
                new CreateIndex( fileCondition1, ScmFulltextMode.async ) );
        threadExec.addWorker(
                new CreateIndex( fileCondition2, ScmFulltextMode.sync ) );
        threadExec.run();

        // 等待索引建立
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 获取工作区索引信息
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );

        // 检查结果
        if ( indexInfo.getMode().equals( ScmFulltextMode.async ) ) {
            Assert.assertEquals( indexInfo.getFileMatcher().toString(),
                    fileCondition1.toString() );
            FullTextUtils.searchAndCheckResults( ws,
                    ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                    fileCondition1 );
        } else {
            Assert.assertEquals( indexInfo.getFileMatcher().toString(),
                    fileCondition2.toString() );
            FullTextUtils.searchAndCheckResults( ws,
                    ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                    fileCondition2 );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
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
            fileIdList1.add( file.save() );
        }

        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setAuthor( authorName2 );
            file.setContent( filePath );
            fileIdList2.add( file.save() );
        }
    }

    private class CreateIndex {
        private BSONObject fileCondition;
        private ScmFulltextMode mode;

        public CreateIndex( BSONObject fileCondition, ScmFulltextMode mode ) {
            this.fileCondition = fileCondition;
            this.mode = mode;
        }

        @ExecuteOrder(step = 1)
        private void create() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.createIndex( ws,
                        new ScmFulltextOption( fileCondition, mode ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FULL_TEXT_INDEX_IS_CREATING ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
