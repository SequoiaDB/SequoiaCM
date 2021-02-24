package com.sequoiacm.fulltextsearch;

import java.io.File;
import java.util.Date;

import org.bson.BSONObject;
import org.testng.Assert;
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
import com.sequoiacm.client.element.fulltext.ScmFileFulltextInfo;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
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
 * @Description: SCM-2994 ::文件未创建索引，更新文件属性匹配file_matcher条件
 * @author fanyu
 * @Date:2020/11/10
 * @version:1.0
 */
public class FullText2994 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file2994";
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private Date minDate = new Date();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath1 = localPath + File.separator + "localFile_" + 1024 + ".txt";
        TestTools.LocalFile.createFile( filePath1, "test...", 1024 );

        filePath2 = localPath + File.separator + "localFile_" + 2048 + ".txt";
        TestTools.LocalFile.createFile( filePath2, "test...", 2048 );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        // 创建文件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( filePath1 );
        file.setCreateTime( minDate );
        fileId = file.save();
        // 创建索引
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).lessThanEquals( "9" )
                .and( ScmAttributeName.File.TITLE ).is( fileName ).get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        // a、更新字段包含匹配条件全部字字段
        updateAttr1();

        // 更新工作区索引条件
        updateFullTextIndex();

        // b、更新字段包含匹配条件部分字段
        updateAttr2();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
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

    private void updateAttr1() throws Exception {
        ScmFile updateFile1 = ScmFactory.File.getInstance( ws, fileId );
        updateFile1.setTitle( fileName );
        updateFile1.setAuthor( "8" );
        // 等待文件索引建立
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 1 );
        // 检查结果
        ScmFileFulltextInfo indexInfo1 = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId );
        Assert.assertEquals( indexInfo1.getStatus(),
                ScmFileFulltextStatus.CREATED );
        BSONObject bson = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( "8" ).and( ScmAttributeName.File.TITLE ).is( fileName )
                .get();
        // 检索和检查文件
        searchAndCheckResults( bson, 1 );
    }

    private void updateFullTextIndex() throws Exception {
        BSONObject updateFileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).lessThanEquals( "9" )
                .and( ScmAttributeName.File.TITLE ).is( fileName )
                .and( ScmAttributeName.File.SIZE ).greaterThan( 1024 ).get();
        ScmFactory.Fulltext.alterIndex( ws, new ScmFulltextModifiler()
                .newFileCondition( updateFileCondition ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    private void updateAttr2() throws Exception {
        // b、更新字段包含匹配条件部分字段
        ScmFile updateFile2 = ScmFactory.File.getInstance( ws, fileId );
        updateFile2.updateContent( filePath2 );

        // 等待文件索引建立
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 2 );

        // 检查结果
        ScmFileFulltextInfo indexInfo2 = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId );
        Assert.assertEquals( indexInfo2.getStatus(),
                ScmFileFulltextStatus.CREATED );
        BSONObject bson1 = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( "8" ).and( ScmAttributeName.File.TITLE ).is( fileName )
                .and( ScmAttributeName.File.SIZE ).greaterThan( 1024 ).get();
        // 检索和检查文件
        searchAndCheckResults( bson1, 1 );
    }

    private void searchAndCheckResults( BSONObject match, int expCount )
            throws ScmException {
        ScmCursor< ScmFulltextSearchResult > fulltextResults1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( match )
                .scope( ScmType.ScopeType.SCOPE_CURRENT ).match( "test" )
                .search();
        int i = 0;
        while ( fulltextResults1.hasNext() ) {
            ScmFulltextSearchResult info = fulltextResults1.getNext();
            Assert.assertEquals( info.getHighlightTexts().size(), 0 );
            ScmFileBasicInfo fileInfo = info.getFileBasicInfo();
            Assert.assertEquals( fileInfo.getFileName(), fileName );
            Assert.assertEquals( fileInfo.getFileId(), fileId );
            Assert.assertEquals( fileInfo.getMimeType(),
                    MimeType.PLAIN.getType() );
            i++;
        }
        Assert.assertEquals( i, expCount );
    }
}