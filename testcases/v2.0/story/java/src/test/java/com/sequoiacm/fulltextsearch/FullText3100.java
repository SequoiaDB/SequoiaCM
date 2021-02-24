package com.sequoiacm.fulltextsearch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
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
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description: SCM-3100:用户自定义查询条件，全文检索
 * @author fanyu
 * @Date:2020/11/16
 * @version:1.0
 */
public class FullText3100 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3100_";
    private int fileNum = 10;
    private List< ScmId > fileIdList = new ArrayList<>();
    private File localPath = null;
    private String content = "test text ";
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + 1024 + ".txt";
        TestTools.LocalFile.createFile( filePath, content, 1024 );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建文件
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
            file.updateContent( filePath );
        }
        // 创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        BSONObject fileCondition1 = new BasicBSONObject();
        BSONObject fulltextCondition1 = ( BasicBSONObject ) JSON.parse(
                "{\"query\" : {\"match\" : {\"fileContent\" : \"text\"}}}" );
        ScmCursor< ScmFulltextSearchResult > actResults1 = ScmFactory.Fulltext
                .customSeracher( ws ).scope( ScmType.ScopeType.SCOPE_ALL )
                .fileCondition( fileCondition1 )
                .fulltextCondition( fulltextCondition1 ).search();
        ScmCursor< ScmFileBasicInfo > expResults1 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_ALL,
                        fileCondition1 );
        FullTextUtils.checkCursor( actResults1, expResults1 );

        BSONObject fileCondition2 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID )
                .is( fileIdList.get( 0 ).get() ).get();
        BSONObject fulltextCondition2 = ( BasicBSONObject ) JSON.parse(
                "{\"query\" : {\"match_phrase\" : {\"fileContent\" : \"test text \"}}}" );
        ScmCursor< ScmFulltextSearchResult > actResults2 = ScmFactory.Fulltext
                .customSeracher( ws ).scope( ScmType.ScopeType.SCOPE_ALL )
                .fileCondition( fileCondition2 )
                .fulltextCondition( fulltextCondition2 ).search();
        ScmCursor< ScmFileBasicInfo > expResults2 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_ALL,
                        fileCondition2 );
        FullTextUtils.checkCursor( actResults2, expResults2 );

        BSONObject fileCondition3 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID )
                .in( fileIdList.get( 0 ).get(), fileIdList.get( 1 ).get() )
                .get();
        BSONObject fulltextCondition3 = ( BasicBSONObject ) JSON.parse(
                "{\"query\" : {\"multi_match\" : {\"query\" : \"test\", \"fields\" : [\"fileContent\"]}}}" );
        ScmCursor< ScmFulltextSearchResult > actResults3 = ScmFactory.Fulltext
                .customSeracher( ws ).scope( ScmType.ScopeType.SCOPE_ALL )
                .fileCondition( fileCondition3 )
                .fulltextCondition( fulltextCondition3 ).search();
        ScmCursor< ScmFileBasicInfo > expResults3 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_ALL,
                        fileCondition3 );
        FullTextUtils.checkCursor( actResults3, expResults3 );

        BSONObject fileCondition4 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID )
                .in( fileIdList.get( 0 ).get() ).get();
        BSONObject fulltextCondition4 = ( BasicBSONObject ) JSON.parse(
                "{\"query\" : {\"bool\" : {\"must\" : [{\"match\" : {\"fileContent\" : \"test\"}}]}}}" );
        ScmCursor< ScmFulltextSearchResult > actResults4 = ScmFactory.Fulltext
                .customSeracher( ws ).scope( ScmType.ScopeType.SCOPE_ALL )
                .fileCondition( fileCondition4 )
                .fulltextCondition( fulltextCondition4 ).search();
        ScmCursor< ScmFileBasicInfo > expResults4 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_ALL,
                        fileCondition4 );
        FullTextUtils.checkCursor( actResults4, expResults4 );

        BSONObject fileCondition5 = new BasicBSONObject();
        BSONObject fulltextCondition5 = ( BasicBSONObject ) JSON.parse(
                "{\"query\" : {\"bool\" : {\"must_not\" : {\"match\" : {\"fileContent\" : \"ok\"}}}}}" );
        ScmCursor< ScmFulltextSearchResult > actResults5 = ScmFactory.Fulltext
                .customSeracher( ws ).scope( ScmType.ScopeType.SCOPE_ALL )
                .fileCondition( fileCondition5 )
                .fulltextCondition( fulltextCondition5 ).search();
        ScmCursor< ScmFileBasicInfo > expResults5 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_ALL,
                        fileCondition5 );
        FullTextUtils.checkCursor( actResults5, expResults5 );

        BSONObject fileCondition6 = new BasicBSONObject();
        BSONObject fulltextCondition6 = ( BasicBSONObject ) JSON.parse(
                "{\"query\" : {\"bool\" : {\"should\" : [{\"match\" : {\"fileContent\" : \"test\"}}]}}}" );
        ScmCursor< ScmFulltextSearchResult > actResults6 = ScmFactory.Fulltext
                .customSeracher( ws ).scope( ScmType.ScopeType.SCOPE_ALL )
                .fileCondition( fileCondition6 )
                .fulltextCondition( fulltextCondition6 ).search();
        ScmCursor< ScmFileBasicInfo > expResults6 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_ALL,
                        fileCondition6 );
        FullTextUtils.checkCursor( actResults6, expResults6 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
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
}