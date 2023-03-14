package com.sequoiacm.fulltextsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.sequoiacm.client.element.fulltext.ScmFileFulltextInfo;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextJobInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description: SCM-3037 :: 符合条件无索引/不符合条件有索引，inspect工作区
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class FullText3037 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3037_";
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private List< ScmId > fileIdList3 = new ArrayList<>();
    private String filePath = null;
    private int fileNum = 20;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.DOC );
        prepareFile();
    }

    @Test
    private void test() throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( fileNameBase ).get();
        // 工作区创建索引
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( condition, ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 制造场景
        makeScene();

        // inspect工作区
        ScmFactory.Fulltext.inspectIndex( ws );

        // 等待工作区索引状态
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 检查工作区索引信息
        checkIndexInfo( condition );

        // 检查文件索引信息
        for ( ScmId fileId : fileIdList1 ) {
            checkFileInfo( fileId, ScmFileFulltextStatus.CREATED );
        }

        for ( ScmId fileId : fileIdList2 ) {
            checkFileInfo( fileId, ScmFileFulltextStatus.NONE );
        }

        for ( ScmId fileId : fileIdList3 ) {
            checkFileInfo( fileId, ScmFileFulltextStatus.CREATED );
        }

        // 全文检索
        List< String > fileIdStrList = new ArrayList<>();
        for ( ScmId fileId : fileIdList1 ) {
            fileIdStrList.add( fileId.get() );
        }
        for ( ScmId fileId : fileIdList3 ) {
            fileIdStrList.add( fileId.get() );
        }
        BSONObject matcher = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdStrList )
                .get();
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject(), matcher );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId fileId : fileIdList3 ) {
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

    private void checkFileInfo( ScmId fileId, ScmFileFulltextStatus status )
            throws ScmException {
        ScmFileFulltextInfo fileInxInfoA1 = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId, 2, 0 );
        ScmFileFulltextInfo fileInxInfoA2 = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId, 1, 0 );
        Assert.assertEquals( fileInxInfoA1.getStatus(), status );
        Assert.assertEquals( fileInxInfoA2.getStatus(), status );
    }

    private void checkIndexInfo( BSONObject expCondition ) throws Exception {
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertEquals( indexInfo.getFileMatcher(), expCondition );
        Assert.assertNotNull( indexInfo.getFulltextLocation() );
        Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.async );
        Assert.assertEquals( indexInfo.getStatus(), ScmFulltextStatus.CREATED );
        ScmFulltextJobInfo jodInfo = indexInfo.getJobInfo();
        while ( jodInfo.getProgress() != 100 ) {
            jodInfo = ScmFactory.Fulltext.getIndexInfo( ws ).getJobInfo();
        }
        try {
            Assert.assertEquals( jodInfo.getEstimateFileCount(),
                    ( fileIdList1.size() + fileIdList2.size() ) * 2 );
            Assert.assertEquals( jodInfo.getSuccessCount(),
                    ( fileIdList1.size() + fileIdList2.size() ) * 2 );
            Assert.assertEquals( jodInfo.getErrorCount(), 0 );
        } catch ( AssertionError e ) {
            throw new Exception( "jodInfo = " + jodInfo.toString(), e );
        }
    }

    private void makeScene() throws Exception {
        // 获取工作区文件元数据表
        String csName = TestSdbTools.getFileMetaCsName( wsName );
        String currClName = "FILE";
        String histClName = "FILE_HISTORY";

        // 符合建索引条件，没有索引
        BSONObject modifiedA1 = new BasicBSONObject( "$set",
                new BasicBSONObject( "external_data.fulltext_status",
                        "NONE" ) );
        BSONObject modifiedB1 = new BasicBSONObject( "$set",
                new BasicBSONObject( "external_data.fulltext_status",
                        "ERROR" ) );
        updateFileIndexStatus( csName, currClName, histClName,
                fileIdList1.subList( 0, fileNum / 2 ), modifiedA1 );
        updateFileIndexStatus( csName, currClName, histClName,
                fileIdList1.subList( fileNum / 2, fileNum ), modifiedB1 );

        // 不符合条件，有索引
        BSONObject modifiedA2 = new BasicBSONObject( "$set",
                new BasicBSONObject( "external_data.fulltext_status",
                        "CREATED" ) );
        BSONObject modifiedB2 = new BasicBSONObject( "$set",
                new BasicBSONObject( "external_data.fulltext_status",
                        "ERROR" ) );
        updateFileIndexStatus( csName, currClName, histClName,
                fileIdList2.subList( 0, fileNum / 2 ), modifiedA2 );
        updateFileIndexStatus( csName, currClName, histClName,
                fileIdList2.subList( fileNum / 2, fileNum ), modifiedB2 );
    }

    private void updateFileIndexStatus( String csName, String currClName,
            String histClName, List< ScmId > fileIdList, BSONObject modified )
            throws Exception {
        for ( ScmId fileId : fileIdList ) {
            BSONObject matcherA1 = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                    .get();
            // update( String urls, String user, String password,
            TestSdbTools.update( TestScmBase.mainSdbUrl,
                    TestScmBase.sdbUserName, TestScmBase.sdbPassword, csName,
                    currClName, matcherA1, modified );
            TestSdbTools.update( TestScmBase.mainSdbUrl,
                    TestScmBase.sdbUserName, TestScmBase.sdbPassword, csName,
                    histClName, matcherA1, modified );
        }
    }

    private void prepareFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + UUID.randomUUID() );
            file.setAuthor( fileNameBase );
            file.setContent( filePath );
            fileIdList1.add( file.save() );
            file.updateContent( filePath );
        }

        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + UUID.randomUUID() );
            file.setContent( filePath );
            fileIdList2.add( file.save() );
            file.updateContent( filePath );
        }

        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + UUID.randomUUID() );
            file.setAuthor( fileNameBase );
            file.setContent( filePath );
            fileIdList3.add( file.save() );
            file.updateContent( filePath );
        }
    }
}