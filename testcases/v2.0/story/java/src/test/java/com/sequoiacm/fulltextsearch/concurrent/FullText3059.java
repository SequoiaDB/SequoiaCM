package com.sequoiacm.fulltextsearch.concurrent;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BasicBSONObject;
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
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3059 :: 并发指定文件重新建索引和增删改文件
 * @author fanyu
 * @Date:2020/9/25
 * @version:1.0
 */
public class FullText3059 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new CopyOnWriteArrayList<>();
    private String fileNameBase = "file3059-";
    private int fileNum = 10;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        prepareFile();
        // 创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Throwable {
        ThreadExecutor threadExec = new ThreadExecutor();
        // 增加文件
        threadExec.addWorker(
                new CreateFile( fileNameBase + UUID.randomUUID().toString() ) );

        // 删除文件和指定文件重新创建索引
        int num = fileIdList.size() / 2;
        for ( int i = 0; i < num / 2; i++ ) {
            ScmId fileId = fileIdList.remove( i );
            threadExec.addWorker( new DeleteFile( fileId ) );
            threadExec.addWorker( new Rebuild( fileId ) );
        }

        // 指定文件重建索引
        for ( int i = 0; i < fileIdList.size() / 2; i++ ) {
            threadExec.addWorker( new Rebuild( fileIdList.get( i ) ) );
        }

        // 更新文件属性
        threadExec.addWorker(
                new UpdateFile( fileIdList.get( 0 ), fileNameBase ) );

        // 更新文件内容
        threadExec.addWorker( new UpdateFileContent( fileIdList.get( 1 ) ) );
        threadExec.run();

        // 全文检索
        FullTextUtils.searchAndCheckResults( ws,
                ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                new BasicBSONObject() );
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
        byte[] bytes = new byte[ 1024 * 200 ];
        new Random().nextBytes( bytes );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList.add( file.save() );
            file.updateContent( new ByteArrayInputStream( bytes ) );
        }
    }

    private class Rebuild {
        private ScmId fileId;

        public Rebuild( ScmId fileId ) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void rebuild() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                try {
                    ScmFactory.Fulltext.rebuildFileIndex( ws, fileId );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
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

    private class CreateFile {
        private String fileName;

        public CreateFile( String fileName ) {
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void create() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName );
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

    private class UpdateFileContent {
        private ScmId fileId;

        public UpdateFileContent( ScmId fileId ) {
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
                byte[] bytes = new byte[ 1024 * 200 ];
                new Random().nextBytes( bytes );
                file.updateContent( new ByteArrayInputStream( bytes ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
