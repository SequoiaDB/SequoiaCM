
package com.sequoiacm.fulltextsearch.concurrent;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
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
 * @Description: SCM-3006 :: 并发ws删除索引和更新文件
 * @author fanyu
 * @Date:2020/9/25
 * @version:1.0
 */
public class FullText3006 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String fileNameBase = "file3006-";
    private int fileNum = 40;
    private String rootDirId = null;
    private String dirName = "/dir3006";
    private String dirId = null;

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
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                .get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Throwable {
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new DropIndex() );
        threadExec.addWorker( new UpdateFile(
                fileIdList.subList( 2 * fileNum / 4, 3 * fileNum / 4 ),
                rootDirId ) );
        threadExec.addWorker( new UpdateFile(
                fileIdList.subList( 4 * fileNum / 4, 4 * fileNum / 4 ),
                rootDirId ) );
        threadExec.run();
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );
        BSONObject condition = ScmQueryBuilder
                .start( "external_data.fulltext_status" ).is( "NONE" ).get();
        long actCount = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        Assert.assertEquals( actCount, fileNum );
        try {
            ScmFactory.Fulltext.simpleSeracher( ws )
                    .fileCondition( new BasicBSONObject() ).notMatch( " " )
                    .search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_DISABLE ) {
                throw e;
            }
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
                ScmFactory.Directory.deleteInstance( ws, dirName );
                ScmFulltexInfo indexInfo = ScmFactory.Fulltext
                        .getIndexInfo( ws );
                if ( !indexInfo.getStatus().equals( ScmFulltextStatus.NONE ) ) {
                    ScmFactory.Fulltext.dropIndex( ws );
                    FullTextUtils.waitWorkSpaceIndexStatus( ws,
                            ScmFulltextStatus.NONE );
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
        }

        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setDirectory( dirId );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList.add( file.save() );
        }
    }

    private class DropIndex {
        @ExecuteOrder(step = 1)
        private void drop() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
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

    private class UpdateFile {
        private List< ScmId > fileIdList;
        private String dirId;

        public UpdateFile( List< ScmId > fileIdList, String dirId ) {
            this.fileIdList = fileIdList;
            this.dirId = dirId;
        }

        @ExecuteOrder(step = 1)
        private void update() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                for ( ScmId fileId : fileIdList ) {
                    ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                    file.setDirectory( dirId );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
