package com.sequoiacm.fulltextsearch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
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
 * @Description SCM-2992:文件已创建索引，更新文件属性不匹配file_matcher条件
 * @author wuyan
 * @Date 2020.09.14
 * @version 1.00
 */

public class UpdateFileAttr2992 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "文件file2992";
    private String wsName = null;

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", fileName );
        matcher.put( "title", fileName );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
    }

    @Test
    private void test() throws Exception {
        fileId = createFile( fileName, fileName, fileName );
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                fileId );

        // 更新属性字段包含匹配条件部分字段
        String author = "file2992";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.setAuthor( author );
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE, fileId );

        seracherFileAndCheckResult();

        // 更新属性字段都不匹配索引条件
        ScmFactory.File.deleteInstance( ws, fileId, true );
        fileId = createFile( fileName, fileName, fileName );
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                fileId );

        String authorB = "file2992";
        String titleB = "titleB";
        ScmFile fileB = ScmFactory.File.getInstance( ws, fileId );
        fileB.setAuthor( authorB );
        fileB.setTitle( titleB );
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE, fileId );
        seracherFileAndCheckResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
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

    private void seracherFileAndCheckResult() throws Exception {
        // 全文检索
        ScmCursor< ScmFulltextSearchResult > result1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .notMatch( "condition" ).search();
        Assert.assertFalse( result1.hasNext() );
    }
}
