package com.sequoiacm.fulltextsearch;

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
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextJobInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description: SCM-3029 :: 工作区创建/更新/删除索引后，重新创建索引
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class FullText3029 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3029_";
    private List< ScmId > fileIdList = new ArrayList<>();
    private String filePath = null;
    private int fileNum = 20;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.DOC );
    }

    @Test
    private void test() throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR )
                .greaterThan( fileNameBase + 0 )
                .and( ScmAttributeName.File.AUTHOR )
                .lessThanEquals( fileNameBase + "1" ).get();
        // 工作区创建索引
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( condition, ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 创建文件
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setAuthor( fileNameBase + i );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }

        // 工作区更新索引
        BSONObject modifiedCondition = new BasicBSONObject();
        ScmFactory.Fulltext.alterIndex( ws,
                new ScmFulltextModifiler().newFileCondition( modifiedCondition )
                        .newMode( ScmFulltextMode.sync ) );

        // 工作区删除索引
        ScmFactory.Fulltext.dropIndex( ws );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );

        // 工作区重新创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 获取工作区索引状态
        checkIndexInfo( new BasicBSONObject() );

        // 全文检索
        FullTextUtils.searchAndCheckResults( ws,
                ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                new BasicBSONObject() );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
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
            Assert.assertEquals( jodInfo.getEstimateFileCount(), fileNum );
            Assert.assertEquals( jodInfo.getSuccessCount(), fileNum );
            Assert.assertEquals( jodInfo.getErrorCount(), 0 );
        } catch ( AssertionError e ) {
            throw new Exception( "jodInfo = " + jodInfo.toString(), e );
        }
    }
}