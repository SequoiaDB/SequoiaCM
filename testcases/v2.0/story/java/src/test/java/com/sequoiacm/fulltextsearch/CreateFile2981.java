package com.sequoiacm.fulltextsearch;

import org.bson.BSONObject;
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
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFileFulltextInfo;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description SCM-2981 :: 新建文件，其中文件属性不匹配file_matcher
 * @author wuyan
 * @Date 2020.09.15
 * @version 1.00
 */

public class CreateFile2981 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileIdA = null;
    private String fileNameA = "file2981A";
    private ScmId fileIdB = null;
    private String fileNameB = "file2981B";
    private String wsName = null;

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", "testusr2981" );
        matcher.put( "title", "title2981" );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
    }

    @Test
    private void test() throws Exception {
        String authorA = "testusr2981";
        String authorB = "LILI2981";
        String titleA = "title2981A";
        String titleB = "title2981B";
        fileIdA = createFile( fileNameA, authorA, titleA );
        fileIdB = createFile( fileNameB, authorB, titleB );
        SearchFileAndCheckResult( fileIdA );
        SearchFileAndCheckResult( fileIdB );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileIdA, true );
                ScmFactory.File.deleteInstance( ws, fileIdB, true );
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

    private ScmId createFile( String fileName, String author, String title )
            throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( author );
        file.setTitle( title );
        file.setContent( filePath );
        ScmId fileId = file.save();
        return fileId;
    }

    private void SearchFileAndCheckResult( ScmId fileId ) throws Exception {
        ScmFileFulltextInfo info = ScmFactory.Fulltext.getFileIndexInfo( ws,
                fileId );
        ScmFileFulltextStatus indexStatus = info.getStatus();
        Assert.assertEquals( indexStatus, ScmFileFulltextStatus.NONE );

        // 全文检索
        ScmCursor< ScmFulltextSearchResult > result1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).notMatch( "condition" )
                .search();

        Assert.assertFalse( result1.hasNext() );
    }
}
