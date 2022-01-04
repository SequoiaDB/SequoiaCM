
package com.sequoiacm.fulltextsearch.concurrent;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3058:: 并发全文检索和增删改查文件
 * @author fanyu
 * @Date:2020/9/25
 * @version:1.0
 */
public class FullText3058 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new CopyOnWriteArrayList<>();
    private String fileNameBase = "file3058-";
    private int fileNum = 100;
    private String rootDirId = null;
    private String dirName = "/dir3058";
    private String dirId = null;
    private BSONObject fileCondition = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        rootDirId = ScmFactory.Directory.getInstance( ws, "/" ).getId();
        dirId = ScmFactory.Directory.createInstance( ws, dirName ).getId();
        prepareFile();
        // 创建索引
        fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                .get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        ScmFactory.Fulltext.inspectIndex( ws );
    }

    @Test
    private void test() throws Throwable {
        ThreadExecutor threadExec = new ThreadExecutor();
        // 全文检索
        BSONObject fulltextCondition = new BasicBSONObject();
        threadExec.addWorker( new Search( ScmType.ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start( ScmAttributeName.File.DIRECTORY_ID )
                        .is( rootDirId ).get(),
                fulltextCondition ) );
        threadExec.addWorker( new Search( ScmType.ScopeType.SCOPE_ALL,
                fulltextCondition, fulltextCondition ) );
        threadExec.addWorker( new Search( ScmType.ScopeType.SCOPE_HISTORY,
                ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                        .is( fileIdList.get( 0 ).get() ).get(),
                new BasicBSONObject() ) );

        // 增加文件符合和不符合索引条件
        threadExec.addWorker( new CreateFile(
                fileNameBase + UUID.randomUUID().toString(), rootDirId ) );
        threadExec.addWorker( new CreateFile(
                fileNameBase + UUID.randomUUID().toString(), dirId ) );

        // 删除文件符合和不符合条件
        threadExec.addWorker( new DeleteFile( fileIdList.remove( 2 ) ) );
        threadExec.addWorker(
                new DeleteFile( fileIdList.remove( fileNum / 2 + 1 ) ) );

        // 更新文件符合和不符合条件
        threadExec.addWorker( new UpdateFile( fileIdList.get( 3 ), dirId ) );
        threadExec.addWorker( new UpdateFile( fileIdList.get( fileNum / 2 + 2 ),
                rootDirId ) );
        // 查询文件
        threadExec.addWorker( new QueryFile( ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject() ) );
        threadExec.run();

        long count = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start( ScmAttributeName.File.DIRECTORY_ID )
                        .is( rootDirId ).get() );

        // 检查文件索引状态
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                ( int ) count * 2 - 1 );

        // 全文检索
        FullTextUtils.searchAndCheckResults( ws,
                ScmType.ScopeType.SCOPE_CURRENT, fileCondition, fileCondition );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                ScmFactory.Directory.deleteInstance( ws, dirName );
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
        byte[] bytes = new byte[ 1024 * 200 ];
        new Random().nextBytes( bytes );
        for ( int i = 0; i < fileNum / 2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setDirectory( rootDirId );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList.add( file.save() );
            file.updateContent( new ByteArrayInputStream( bytes ) );
        }

        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setDirectory( dirId );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList.add( file.save() );
            file.updateContent( new ByteArrayInputStream( bytes ) );
        }
    }

    private class Search {
        private ScmType.ScopeType scopeType;
        private BSONObject fileCondition;
        private BSONObject fulltextCondition;

        public Search( ScmType.ScopeType scopeType, BSONObject fileCondition,
                BSONObject fulltextCondition ) {
            this.scopeType = scopeType;
            this.fileCondition = fileCondition;
            this.fulltextCondition = fulltextCondition;
        }

        @ExecuteOrder(step = 1)
        private void search() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmCursor< ScmFulltextSearchResult > cursor = ScmFactory.Fulltext
                        .customSeracher( ws ).fileCondition( fileCondition )
                        .fulltextCondition( fulltextCondition )
                        .scope( scopeType ).search();
                int i = 0;
                while ( cursor.hasNext() ) {
                    ScmFulltextSearchResult scmFulltextSearchResult = cursor
                            .getNext();
                    Assert.assertEquals(
                            scmFulltextSearchResult.getHighlightTexts().size(),
                            0 );
                    Assert.assertNotNull(
                            scmFulltextSearchResult.getFileBasicInfo() );
                    i++;
                }
                Assert.assertEquals( i > 0, true );

            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CreateFile {
        private String fileName;
        private String dirId;

        public CreateFile( String fileName, String dirId ) {
            this.fileName = fileName;
            this.dirId = dirId;
        }

        @ExecuteOrder(step = 1)
        private void create() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName );
                file.setDirectory( dirId );
                byte[] bytes = new byte[ 1024 * 200 ];
                new Random().nextBytes( bytes );
                file.setMimeType( MimeType.PLAIN );
                file.setContent( new ByteArrayInputStream( bytes ) );
                fileIdList.add( file.save() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFile {
        private ScmId fileId;

        public DeleteFile( ScmId fileId ) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void delete() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.File.deleteInstance( ws, fileId, true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFile {
        private ScmId fileId;
        private String dirId;

        public UpdateFile( ScmId fileId, String dirId ) {
            this.fileId = fileId;
            this.dirId = dirId;
        }

        @ExecuteOrder(step = 1)
        private void update() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setDirectory( dirId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class QueryFile {
        private ScmType.ScopeType scopeType;
        private BSONObject fileCondition;

        public QueryFile( ScmType.ScopeType scopeType,
                BSONObject fileCondition ) {
            this.scopeType = scopeType;
            this.fileCondition = fileCondition;
        }

        @ExecuteOrder(step = 1)
        private void query() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                        .listInstance( ws, scopeType, fileCondition );
                // 无法比较结果
                int i = 0;
                while ( cursor.hasNext() ) {
                    ScmFileBasicInfo fileInfo = cursor.getNext();
                    Assert.assertEquals(
                            fileInfo.getFileName().startsWith( fileNameBase ),
                            true, fileInfo.toString() );
                    i++;
                }
                Assert.assertEquals( i > 0, true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
