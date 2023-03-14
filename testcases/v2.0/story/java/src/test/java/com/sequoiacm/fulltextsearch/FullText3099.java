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
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.common.MimeType;
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
 * @Description: SCM-3099 :: 覆盖上传文件创建索引
 * @author fanyu
 * @Date:2020/11/16
 * @version:1.0
 */
public class FullText3099 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file3099";
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath1 = localPath + File.separator + "localFile_" + 99 + ".txt";
        TestTools.LocalFile.createFile( filePath1, "AAAA", 99 );
        filePath2 = localPath + File.separator + "localFile_" + 1024 + ".txt";
        TestTools.LocalFile.createFile( filePath2, "test...", 1024 );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.SIZE ).greaterThan( 100 )
                .and( ScmAttributeName.File.TITLE ).is( fileName ).get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        // 创建文件,不符合工作区索引条件
        ScmFile file1 = ScmFactory.File.createInstance( ws );
        file1.setFileName( fileName );
        file1.setTitle( fileName );
        file1.setContent( filePath1 );
        fileId = file1.save();

        // 覆盖文件，符合工作区索引条件
        ScmFile file2 = ScmFactory.File.createInstance( ws );
        file2.setTitle( fileName );
        file2.setFileName( fileName );
        file2.setContent( filePath2 );
        fileId = file2.save( new ScmUploadConf( true ) );

        // 检查文件索引状态
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 1 );
        checkResults();
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

    private void checkResults() throws Exception {
        // 检索文件
        ScmCursor< ScmFulltextSearchResult > fulltextResults1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).match( "test" )
                .search();
        int i = 0;
        while ( fulltextResults1.hasNext() ) {
            ScmFulltextSearchResult info = fulltextResults1.getNext();
            Assert.assertEquals( info.getHighlightTexts().size(), 0 );
            ScmFileBasicInfo fileInfo = info.getFileBasicInfo();
            Assert.assertEquals( fileInfo.getFileName(), fileName );
            Assert.assertEquals( fileInfo.getFileId(), fileId );
            Assert.assertEquals( fileInfo.getMajorVersion(), 1 );
            Assert.assertEquals( fileInfo.getMimeType(),
                    MimeType.PLAIN.getType() );
            i++;
        }
        Assert.assertEquals( i, 1 );
    }
}