
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
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextJobInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3056:: 并发不同工作区增删改查索引
 * @author fanyu
 * @Date:2020/11/18
 * @version:1.0
 */
public class FullText3056 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName1 = null;
    private String wsName2 = null;
    private String wsName3 = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;
    private ScmWorkspace ws3 = null;
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private List< ScmId > fileIdList3 = new ArrayList<>();
    private String fileNameBase = "file3056-";
    private String authorName1 = "author3056A";
    private String authorName2 = "author3056B";
    private int fileNum = 20;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName1 = WsPool.get();
        wsName2 = WsPool.get();
        wsName3 = WsPool.get();
        ws1 = ScmFactory.Workspace.getWorkspace( wsName1, session );
        ws2 = ScmFactory.Workspace.getWorkspace( wsName2, session );
        ws3 = ScmFactory.Workspace.getWorkspace( wsName3, session );

        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.TEXT );
        prepareFile( ws1, fileIdList1 );
        prepareFile( ws2, fileIdList2 );
        prepareFile( ws3, fileIdList3 );
        BSONObject fileCondition1 = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName1 ).get();
        BSONObject fileCondition2 = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName2 ).get();
        ScmFactory.Fulltext.createIndex( ws1, new ScmFulltextOption(
                fileCondition1, ScmFulltextMode.async ) );
        ScmFactory.Fulltext.createIndex( ws2,
                new ScmFulltextOption( fileCondition2, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws1,
                ScmFulltextStatus.CREATED );
        FullTextUtils.waitWorkSpaceIndexStatus( ws2,
                ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Throwable {
        ThreadExecutor threadExec = new ThreadExecutor();
        // 创建索引
        threadExec.addWorker( new CreateIndex( wsName3, new BasicBSONObject(),
                ScmFulltextMode.async ) );
        // 删除索引
        threadExec.addWorker( new DropIndex( wsName2 ) );
        // 更新索引
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName2 ).get();
        threadExec.addWorker( new UpdateIndex( wsName1, fileCondition,
                ScmFulltextMode.sync ) );
        threadExec.run();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws1, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws2, fileId, true );
                }
                for ( ScmId fileId : fileIdList3 ) {
                    ScmFactory.File.deleteInstance( ws3, fileId, true );
                }
                ScmFactory.Fulltext.dropIndex( ws3 );
                ScmFactory.Fulltext.dropIndex( ws1 );
                FullTextUtils.waitWorkSpaceIndexStatus( ws3,
                        ScmFulltextStatus.NONE );
                FullTextUtils.waitWorkSpaceIndexStatus( ws1,
                        ScmFulltextStatus.NONE );
            }
        } finally {
            if ( wsName1 != null ) {
                WsPool.release( wsName1 );
            }
            if ( wsName2 != null ) {
                WsPool.release( wsName2 );
            }
            if ( wsName3 != null ) {
                WsPool.release( wsName3 );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareFile( ScmWorkspace ws, List< ScmId > fileIdList )
            throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            if ( i % 2 == 0 ) {
                file.setAuthor( authorName1 );
            } else {
                file.setAuthor( authorName2 );
            }
            file.setContent( filePath );
            fileIdList.add( file.save() );
        }
    }

    private class CreateIndex {
        private String wsName;
        private BSONObject fileCondition;
        private ScmFulltextMode mode;

        public CreateIndex( String wsName, BSONObject fileCondition,
                ScmFulltextMode mode ) {
            this.wsName = wsName;
            this.fileCondition = fileCondition;
            this.mode = mode;
        }

        @ExecuteOrder(step = 1)
        private void create() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.createIndex( ws,
                        new ScmFulltextOption( fileCondition, mode ) );
                FullTextUtils.waitWorkSpaceIndexStatus( ws,
                        ScmFulltextStatus.CREATED );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        @ExecuteOrder(step = 2)
        private void check() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                // 检查工作区索引信息
                ScmFulltexInfo info = ScmFactory.Fulltext.getIndexInfo( ws );
                Assert.assertEquals( info.getFileMatcher(), fileCondition,
                        wsName );
                Assert.assertNotNull( info.getFulltextLocation() );
                Assert.assertEquals( info.getMode(), mode );
                ScmFulltextJobInfo jodInfo = info.getJobInfo();
                while ( jodInfo.getProgress() != 100 ) {
                    jodInfo = ScmFactory.Fulltext.getIndexInfo( ws )
                            .getJobInfo();
                }
                long count = ScmFactory.File.countInstance( ws,
                        ScmType.ScopeType.SCOPE_CURRENT, fileCondition );
                Assert.assertEquals( jodInfo.getEstimateFileCount(), count );
                Assert.assertEquals( jodInfo.getSuccessCount(), count );
                Assert.assertEquals( jodInfo.getErrorCount(), 0 );
                Assert.assertNotNull( jodInfo.getSpeed() );

                // 全文检索
                FullTextUtils.searchAndCheckResults( ws,
                        ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                        fileCondition );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateIndex {
        private String wsName;
        private BSONObject fileCondition;
        private ScmFulltextMode mode;

        public UpdateIndex( String wsName, BSONObject fileCondition,
                ScmFulltextMode mode ) {
            this.wsName = wsName;
            this.fileCondition = fileCondition;
            this.mode = mode;
        }

        @ExecuteOrder(step = 1)
        private void update() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.alterIndex( ws, new ScmFulltextModifiler()
                        .newFileCondition( fileCondition ).newMode( mode ) );
                FullTextUtils.waitWorkSpaceIndexStatus( ws,
                        ScmFulltextStatus.CREATED );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        @ExecuteOrder(step = 2)
        private void check() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                // 检查工作区索引信息
                ScmFulltexInfo info = ScmFactory.Fulltext.getIndexInfo( ws );
                Assert.assertEquals( info.getFileMatcher().toString(),
                        fileCondition.toString() );
                Assert.assertNotNull( info.getFulltextLocation() );
                Assert.assertEquals( info.getMode(), mode );
                ScmFulltextJobInfo jodInfo = info.getJobInfo();
                while ( jodInfo.getProgress() != 100 ) {
                    jodInfo = ScmFactory.Fulltext.getIndexInfo( ws )
                            .getJobInfo();
                }
                Assert.assertNotNull( jodInfo.getEstimateFileCount() );
                Assert.assertNotNull( jodInfo.getSuccessCount() );
                Assert.assertEquals( jodInfo.getErrorCount(), 0 );
                Assert.assertNotNull( jodInfo.getSpeed() );

                // 全文检索
                FullTextUtils.searchAndCheckResults( ws,
                        ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                        fileCondition );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DropIndex {
        private String wsName;

        public DropIndex( String wsName ) {
            this.wsName = wsName;
        }

        @ExecuteOrder(step = 1)
        private void drop() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.dropIndex( ws );
                FullTextUtils.waitWorkSpaceIndexStatus( ws,
                        ScmFulltextStatus.NONE );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        @ExecuteOrder(step = 2)
        private void check() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                // 检查工作区索引信息
                ScmFulltexInfo info = ScmFactory.Fulltext.getIndexInfo( ws );
                Assert.assertNull( info.getFileMatcher() );
                Assert.assertNull( info.getFulltextLocation() );
                Assert.assertNull( info.getJobInfo() );
                Assert.assertNull( info.getMode() );
                // 全文检索
                try {
                    ScmFactory.Fulltext.simpleSeracher( ws )
                            .match( "condition" )
                            .fileCondition( new BasicBSONObject() ).search();
                    Assert.fail( "exp fail but act success!!!" );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.FULL_TEXT_INDEX_DISABLE ) {
                        throw e;
                    }
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
