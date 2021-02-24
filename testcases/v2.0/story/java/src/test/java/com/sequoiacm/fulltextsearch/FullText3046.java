package com.sequoiacm.fulltextsearch;

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
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description: SCM-3046 :: 文件符合条件未创建过索引，指定文件重新建索引 SCM-3049 :: 重复多次指定文件建索引
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class FullText3046 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3046_";
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.DOC );
        ScmFile file1 = ScmFactory.File.createInstance( ws );
        file1.setFileName( fileNameBase + 1 );
        file1.setContent( filePath );
        fileId1 = file1.save();
        file1.updateContent( filePath );

        ScmFile file2 = ScmFactory.File.createInstance( ws );
        file2.setFileName( fileNameBase + 2 );
        file2.setContent( filePath );
        fileId2 = file2.save();
        file2.updateContent( filePath );
    }

    @Test
    private void test() throws Exception {
        // 工作区创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 制造场景
        makeScene();

        // 指定文件重建索引
        for ( int i = 0; i < 3; i++ ) {
            ScmFactory.Fulltext.rebuildFileIndex( ws, fileId1 );
            ScmFactory.Fulltext.rebuildFileIndex( ws, fileId2 );
        }

        // 检查文件索引状态信息
        checkFileInfo( fileId1, ScmFileFulltextStatus.CREATED );
        checkFileInfo( fileId2, ScmFileFulltextStatus.CREATED );

        // 全文检索
        BSONObject matcher = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID )
                .in( fileId1.get(), fileId2.get() ).get();
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                matcher, matcher );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId1, true );
                ScmFactory.File.deleteInstance( ws, fileId2, true );
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

    private void makeScene() throws Exception {
        // 获取工作区文件元数据表
        String csName = TestSdbTools.getFileMetaCsName( wsName );
        String currClName = "FILE";
        String histClName = "FILE_HISTORY";

        // 符合建索引条件，没有索引
        BSONObject matcherA1 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId1.get() )
                .get();
        BSONObject matcherB1 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId2.get() )
                .get();
        BSONObject modifiedA1 = new BasicBSONObject( "$set",
                new BasicBSONObject( "external_data.fulltext_status",
                        "NONE" ) );
        BSONObject modifiedB1 = new BasicBSONObject( "$set",
                new BasicBSONObject( "external_data.fulltext_status",
                        "ERROR" ) );

        // update( String urls, String user, String password,
        TestSdbTools.update( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword, csName, currClName, matcherA1,
                modifiedA1 );
        TestSdbTools.update( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword, csName, histClName, matcherA1,
                modifiedA1 );

        TestSdbTools.update( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword, csName, currClName, matcherB1,
                modifiedA1 );
        TestSdbTools.update( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword, csName, histClName, matcherB1,
                modifiedB1 );
    }
}