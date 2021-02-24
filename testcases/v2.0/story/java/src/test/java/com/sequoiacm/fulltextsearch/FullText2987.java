package com.sequoiacm.fulltextsearch;

import java.io.File;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFileFulltextInfo;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
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
 * @Description: SCM-2987 :: 更新文件不创建索引，其中历史版本文件未创建索引 s
 * @author fanyu
 * @Date:2020/11/16
 * @version:1.0
 */
public class FullText2987 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file2987";
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
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.SIZE ).greaterThan( 200 * 1024 )
                .and( ScmAttributeName.File.TITLE ).is( fileName ).get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        // 创建文件,不符合工作区索引条件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setTitle( fileName + "A" );
        file.setContent( filePath1 );
        fileId = file.save();

        // 更新文件属性及文件内容, 不符合工作区条件
        ScmFile updateFile1 = ScmFactory.File.getInstance( ws, fileId );
        updateFile1.updateContent( filePath2 );
        updateFile1.setTitle( fileName );

        // 检查文件索引状态
        ScmFileFulltextInfo histInfo = ScmFactory.Fulltext.getFileIndexInfo( ws,
                fileId, 1, 0 );
        Assert.assertEquals( histInfo.getStatus(), ScmFileFulltextStatus.NONE );
        ScmFileFulltextInfo currInfo = ScmFactory.Fulltext.getFileIndexInfo( ws,
                fileId, 2, 0 );
        Assert.assertEquals( currInfo.getStatus(), ScmFileFulltextStatus.NONE );

        // 检索文件
        // 使用最新版本文件内容检索文件
        ScmCursor< ScmFulltextSearchResult > fulltextResults1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).match( "B" ).search();
        Assert.assertFalse( fulltextResults1.hasNext() );
        // 使用历史版本文件内容检索文件
        ScmCursor< ScmFulltextSearchResult > fulltextResults2 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).match( "A" ).search();
        Assert.assertFalse( fulltextResults2.hasNext() );
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