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
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3063 :: 并发指定文件重新建索引和工作区更新索引
 * @author fanyu
 * @Date:2020/11/17
 * @version:1.0
 */
public class FullText3063 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private List< String > rootDirFileIdList = new ArrayList<>();
    private List< String > dirFileIdList = new ArrayList<>();
    private String fileNameBase = "file3063-";
    private int fileNum = 30;
    private String dirName = "/dir3063";
    private String rootDirId = null;
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
        // 制造符合条件却无索引和不符合条件且有索引的场景
        makeScene();
    }

    @Test
    private void test() throws Throwable {
        ThreadExecutor threadExec = new ThreadExecutor();
        // 重建索引
        for ( String fileId : rootDirFileIdList ) {
            threadExec.addWorker( new Rebuild( new ScmId( fileId ) ) );
        }

        for ( String fileId : dirFileIdList ) {
            threadExec.addWorker( new Rebuild( new ScmId( fileId ) ) );
        }
        // 更新索引
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( dirId ).get();
        threadExec.addWorker(
                new UpdateIndex( ScmFulltextMode.async, fileCondition ) );
        threadExec.run();

        // 检查结果
        checkResults();
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

    private void checkResults() throws Exception {
        // 等待索引状态为CREATED
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        ScmFactory.Fulltext.inspectIndex( ws );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        BSONObject condition1 = ScmQueryBuilder
                .start( "external_data.fulltext_status" ).is( "NONE" ).get();
        long actCount1 = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition1 );
        Assert.assertEquals( actCount1, fileNum );

        BSONObject condition2 = ScmQueryBuilder
                .start( "external_data.fulltext_status" ).is( "CREATED" ).get();
        long actCount2 = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition2 );
        Assert.assertEquals( actCount2, fileNum );

        BSONObject condition3 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( dirFileIdList )
                .get();
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject(), condition3 );
    }

    private void prepareFile() throws ScmException {
        byte[] bytes = new byte[ 1024 * 200 ];
        new Random().nextBytes( bytes );
        for ( int i = 0; i < fileNum / 2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setContent( new ByteArrayInputStream( bytes ) );
            file.setDirectory( rootDirId );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
            rootDirFileIdList.add( fileId.get() );
            file.updateContent( new ByteArrayInputStream( bytes ) );
        }
        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setContent( new ByteArrayInputStream( bytes ) );
            file.setDirectory( dirId );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
            dirFileIdList.add( fileId.get() );
            file.updateContent( new ByteArrayInputStream( bytes ) );
        }
    }

    private void makeScene() throws Exception {
        // 制造符合条件却无索引的场景
        BSONObject modifier1 = new BasicBSONObject( "$set",
                new BasicBSONObject( "external_data",
                        new BasicBSONObject( "fulltext_document_id", null )
                                .append( "fulltext_error", null )
                                .append( "fulltext_status", "NONE" ) ) );
        makeCurrTrouble( new BasicBSONObject( "directory_id", rootDirId ),
                modifier1 );
        makeHisTrouble( ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .in( rootDirFileIdList ).get(), modifier1 );
        // 制造不符合条件确有索引的场景
        BSONObject modifier2 = new BasicBSONObject( "$set", new BasicBSONObject(
                "external_data",
                new BasicBSONObject( "fulltext_document_id",
                        "UvojonQBLzCDZISro1ti" )
                                .append( "fulltext_error", null )
                                .append( "fulltext_status", "ERROR" ) ) );
        makeCurrTrouble( new BasicBSONObject( "directory_id", dirId ),
                modifier2 );
        makeHisTrouble( ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .in( dirFileIdList ).get(), modifier2 );
    }

    private void makeCurrTrouble( BSONObject matcher, BSONObject modifier )
            throws Exception {
        String metaCSName = TestSdbTools.getFileMetaCsName( wsName );
        TestSdbTools.update( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword, metaCSName, "FILE", matcher,
                modifier );
    }

    private void makeHisTrouble( BSONObject matcher, BSONObject modifier )
            throws Exception {
        String metaCSName = TestSdbTools.getFileMetaCsName( wsName );
        TestSdbTools.update( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword, metaCSName, "FILE_HISTORY", matcher,
                modifier );
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
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.rebuildFileIndex( ws, fileId );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_NOT_MEET_WORKSPACE_INDEX_MATCHER ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateIndex {
        private ScmFulltextMode mode;
        private BSONObject fileCondition;

        public UpdateIndex( ScmFulltextMode mode, BSONObject fileCondition ) {
            this.mode = mode;
            this.fileCondition = fileCondition;
        }

        @ExecuteOrder(step = 1)
        private void update() throws ScmException {
            ScmFactory.Fulltext.alterIndex( ws, new ScmFulltextModifiler()
                    .newMode( mode ).newFileCondition( fileCondition ) );
        }
    }
}
