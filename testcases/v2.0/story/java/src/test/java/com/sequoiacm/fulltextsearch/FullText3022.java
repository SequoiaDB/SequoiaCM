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
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.infrastructure.fulltext.core.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-3022 ::工作区更新索引模式
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class FullText3022 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String filePath = null;
    private String fileNameBase = "file3022_";
    private int fileNum = 20;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.XLSX );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    filePath );
            fileIdList.add( fileId );
        }
    }

    @Test
    private void test() throws Exception {
        // 工作区创建索引,模式异步
        BSONObject condition = new BasicBSONObject();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( condition, ScmFulltextMode.async ) );

        // 等待工作区索引建立
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 创建多个文件
        for ( int i = fileNum; i < 2 * fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    filePath );
            fileIdList.add( fileId );
        }

        // 工作区更新索引模式，同步
        ScmFactory.Fulltext.alterIndex( ws,
                new ScmFulltextModifiler().newMode( ScmFulltextMode.sync )
                        .newFileCondition( condition ) );

        // 创建多个文件
        for ( int i = 2 * fileNum; i < 3 * fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    filePath );
            fileIdList.add( fileId );
        }

        // 检查工作区索引信息
        checkIndexInfo( condition );

        // 检查已建索引文件个数
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                3 * fileNum );

        // 全文检索
        FullTextUtils.searchAndCheckResults( ws,
                ScmType.ScopeType.SCOPE_CURRENT, condition, condition );
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
        Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.sync );
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