package com.sequoiacm.fulltextsearch;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2990 :: 多次更新文件创建索引，删除文件
 * @author fanyu
 * @Date:2020/11/16
 * @version:1.0
 */
public class FullText2990 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file2990";
    private int versionNum = 200;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        String filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.TEXT );
        // 创建文件
        fileId = ScmFileUtils.create( ws, fileName, filePath );

        // 更新文件內容
        ScmFile updateFile = ScmFactory.File.getInstance( ws, fileId );
        for ( int i = 0; i < versionNum - 1; i++ ) {
            updateFile.updateContent( filePath );
        }
        // 检查结果
        checkResults();

        // 删除文件
        ScmFactory.File.deleteInstance( ws, fileId, true );

        // 检索文件
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 0 );
        ScmCursor< ScmFulltextSearchResult > fulltextResults = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).notMatch( "condition" )
                .search();
        Assert.assertFalse( fulltextResults.hasNext() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
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
        // 检查文件索引状态
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                versionNum );
        // 检索文件
        ScmCursor< ScmFulltextSearchResult > fulltextResults1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).notMatch( "condition" )
                .search();
        int i = 0;
        while ( fulltextResults1.hasNext() ) {
            ScmFulltextSearchResult info = fulltextResults1.getNext();
            Assert.assertEquals( info.getHighlightTexts().size(), 0 );
            ScmFileBasicInfo fileInfo = info.getFileBasicInfo();
            Assert.assertEquals( fileInfo.getFileName(), fileName );
            i++;
        }
        Assert.assertEquals( i, versionNum );
    }
}