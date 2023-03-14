package com.sequoiacm.fulltextsearch;

import java.io.File;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFileFulltextInfo;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
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

/**
 * @Description: SCM-2993 :: 文件未创建索引，更新文件属性不匹配file_matcher条件
 * @author fanyu
 * @Date:2020/11/10
 * @version:1.0
 */
public class FullText2993 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file2993";
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath1 = localPath + File.separator + "localFile_" + 1024 + ".txt";
        TestTools.LocalFile.createFile( filePath1, fileName + "A", 1024 );
        filePath2 = localPath + File.separator + "localFile_" + 99 + ".txt";
        TestTools.LocalFile.createFile( filePath2, fileName + "B", 99 );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).greaterThan( 9 )
                .and( ScmAttributeName.File.TITLE ).is( fileName ).get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        // 创建文件,不符合工作区索引条件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( "1" );
        file.setTitle( fileName + "A" );
        file.setContent( filePath1 );
        fileId = file.save();
    }

    @Test
    private void test() throws Exception {
        // a、更新属性字段包含匹配条件全部字段,不符合工作区索引条件
        ScmFile updateFile1 = ScmFactory.File.getInstance( ws, fileId );
        updateFile1.setTitle( fileName );
        updateFile1.setAuthor( "2" );
        ScmFileFulltextInfo indexInfo1 = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId );
        // 检查结果
        Assert.assertEquals( indexInfo1.getStatus(),
                ScmFileFulltextStatus.NONE );
        Assert.assertEquals( updateFile1.getTitle(), fileName );
        Assert.assertEquals( updateFile1.getAuthor(), "2" );

        // b、更新属性字段包含匹配条件部分字段
        ScmFile updateFile2 = ScmFactory.File.getInstance( ws, fileId );
        updateFile2.updateContent( filePath2 );
        updateFile2.setAuthor( "3" );
        ScmFileFulltextInfo indexInfo2 = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId );
        ScmFileFulltextInfo indexInfo3 = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId, 1, 0 );
        // 检查结果
        Assert.assertEquals( indexInfo2.getStatus(),
                ScmFileFulltextStatus.NONE );
        Assert.assertEquals( indexInfo3.getStatus(),
                ScmFileFulltextStatus.NONE );
        Assert.assertEquals( updateFile2.getTitle(), fileName );
        Assert.assertEquals( updateFile2.getSize(),
                new File( filePath2 ).length() );
        Assert.assertEquals( updateFile2.getAuthor(), "3" );

        // c、更新属性字段不包含匹配条件字段
        // 检查文件索引状态
        ScmFile updateFile3 = ScmFactory.File.getInstance( ws, fileId );
        updateFile3.updateContent( filePath1 );
        ScmFileFulltextInfo indexInfo4 = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId );
        Assert.assertEquals( indexInfo4.getStatus(),
                ScmFileFulltextStatus.NONE );
        Assert.assertEquals( updateFile3.getSize(),
                new File( filePath1 ).length() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Fulltext.dropIndex( ws );
                FullTextUtils.waitWorkSpaceIndexStatus( ws,
                        ScmFulltextStatus.NONE );
                TestTools.LocalFile.removeFile( localPath );
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