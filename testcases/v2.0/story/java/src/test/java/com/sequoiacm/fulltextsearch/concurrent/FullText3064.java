
package com.sequoiacm.fulltextsearch.concurrent;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3064 :: 并发inspect工作区和增删改文件
 * @author fanyu
 * @Date:2020/9/25
 * @version:1.0
 */
public class FullText3064 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList1 = new CopyOnWriteArrayList<>();
    private List< ScmId > fileIdList2 = new CopyOnWriteArrayList<>();
    private String fileNameBase = "file3064-";
    private String authorName1 = "auth3064A";
    private String authorName2 = "auth3064B";
    private int fileNum = 20;
    byte[] bytes = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        prepareFile();
        new Random().nextBytes( bytes );
        // 创建索引
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName1 ).get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        makeScene();
    }

    @Test
    private void test() throws Throwable {
        ThreadExecutor threadExec = new ThreadExecutor();
        // inspect工作区
        threadExec.addWorker( new Inspect() );

        // 增加文件符合和不符合索引条件
        for ( int i = 0; i < 10; i++ ) {
            threadExec.addWorker( new CreateFile( authorName1, fileIdList1 ) );
            threadExec.addWorker( new CreateFile( authorName2, fileIdList2 ) );
        }

        // 删除文件符合和不符合条件
        for ( int i = 0; i < 10; i++ ) {
            threadExec.addWorker( new DeleteFile( fileIdList1.remove( 0 ) ) );
            threadExec.addWorker( new DeleteFile( fileIdList2.remove( 0 ) ) );
        }

        // 更新文件符合和不符合条件
        for ( int i = 10; i < 20; i++ ) {
            ScmId fileId1 = fileIdList1.remove( i );
            ScmId fileId2 = fileIdList2.remove( i );
            fileIdList1.add( fileId2 );
            fileIdList2.add( fileId1 );
            threadExec.addWorker( new UpdateFile( fileId1, authorName2 ) );
            threadExec.addWorker( new UpdateFile( fileId2, authorName1 ) );
        }

        // 更新文件内容
        for ( int i = 20; i < 30; i++ ) {
            threadExec.addWorker( new UpdateContent( fileIdList1.get( i ) ) );
        }
        threadExec.run();

        // 上述并发有可能出现不符合索引条件且有索引的现象，需要重新inspect
        ScmFactory.Fulltext.inspectIndex( ws );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 检查文件索引状态
        List< String > fileIdStrList = new ArrayList<>();
        for ( ScmId fileId : fileIdList1 ) {
            fileIdStrList.add( fileId.get() );
        }
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdStrList )
                .get();
        int expCount = ( int ) ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                expCount );

        // 全文检索
        FullTextUtils.searchAndCheckResults( ws,
                ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                condition );
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

    private void makeScene() throws Exception {
        String metaCSName = TestSdbTools.getFileMetaCsName( wsName );
        // 制造符合条件却无索引的场景
        BSONObject matcher1 = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName1 ).get();
        BSONObject modifier1 = new BasicBSONObject( "$set",
                new BasicBSONObject( "external_data",
                        new BasicBSONObject( "fulltext_document_id", null )
                                .append( "fulltext_error", null )
                                .append( "fulltext_status", "NONE" ) ) );
        TestSdbTools.update( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword, metaCSName, "FILE", matcher1,
                modifier1 );
    }

    private void prepareFile() throws ScmException {

        new Random().nextBytes( bytes );
        for ( int i = 0; i < fileNum / 2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setAuthor( authorName1 );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList1.add( file.save() );
        }

        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setAuthor( authorName2 );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList2.add( file.save() );
        }
    }

    private class Inspect {
        @ExecuteOrder(step = 1)
        private void inspect() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.inspectIndex( ws );
                FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CreateFile {
        private String author;
        private List< ScmId > fileIdList;

        public CreateFile( String author, List< ScmId > fileIdList ) {
            this.author = author;
            this.fileIdList = fileIdList;
        }

        @ExecuteOrder(step = 1)
        private void create() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileNameBase + UUID.randomUUID() );
                file.setAuthor( author );
                byte[] bytes = new byte[ 1024 * 200 ];
                new Random().nextBytes( bytes );
                file.setMimeType( MimeType.PLAIN );
                file.setContent( new ByteArrayInputStream( bytes ) );
                ScmId fileId = file.save();
                fileIdList.add( fileId );
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
                session = ScmSessionUtils.createSession( site );
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

    private class UpdateContent {
        private ScmId fileId;

        public UpdateContent( ScmId fileId ) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void update() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.updateContent( new ByteArrayInputStream( bytes ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFile {
        private ScmId fileId;
        private String author;

        public UpdateFile( ScmId fileId, String author ) {
            this.fileId = fileId;
            this.author = author;
        }

        @ExecuteOrder(step = 1)
        private void update() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setAuthor( author );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
