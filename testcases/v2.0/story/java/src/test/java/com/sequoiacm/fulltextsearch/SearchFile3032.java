package com.sequoiacm.fulltextsearch;

import java.io.File;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description SCM-3032:全文检索条件匹配的文件和文件属性条件匹配的文件无交集，全文检索
 * @author wuyan
 * @Date 2020.09.21
 * @version 1.00
 */

public class SearchFile3032 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file3032";
    private String wsName = null;
    private File localPath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        String filePath1 = localPath + File.separator + "localFile_" + 1024
                + ".txt";
        TestTools.LocalFile.createFile( filePath1, "SequoiaDB", 1024 );
        String filePath2 = localPath + File.separator + "localFile_" + 1024 * 2
                + ".txt";
        TestTools.LocalFile.createFile( filePath2, "SequoiaDBtest", 1024 * 2 );

        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "name", fileName );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );

        // 创建多版本文件
        fileId = ScmFileUtils.create( ws, fileName, filePath1 );
        updateFilesContent( filePath1 );
        updateFilesContent( filePath2 );
        updateFilesContent( filePath1 );

        int expCount = 4;
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                expCount );
    }

    @Test
    private void test() throws Exception {
        // 全文检索条件匹配所有文件，文件属性匹配0个文件
        String matchContent = "SequoiaDB";
        BSONObject matcherA = new BasicBSONObject();
        BSONObject valueA = new BasicBSONObject();
        valueA.put( "$lt", 1024 );
        matcherA.put( "size", valueA );
        searchFileAndCheckResult( matcherA, matchContent );

        // 全文检索条件匹配部分文件，文件属性匹配部分个文件，匹配条件无交集
        String matcheContentB = "SequoiaDBtest";
        BSONObject matcherB = new BasicBSONObject();
        BSONObject valueB = new BasicBSONObject();
        valueB.put( "$lte", 1024 );
        matcherB.put( "size", valueB );
        searchFileAndCheckResult( matcherB, matcheContentB );

        // 全文检索条件匹配0文件，文件属性匹配部分文件(属性匹配v3、v4版本文件)
        String mismatchContent = "SequoiaDBbbb";
        BSONObject matcherC = new BasicBSONObject();
        BSONObject valueC = new BasicBSONObject();
        valueC.put( "$gt", 2 );
        matcherC.put( "major_version", valueC );
        searchFileAndCheckResult( matcherC, mismatchContent );
        // 全文检索条件匹配0文件，文件属性匹配0个文件
        searchFileAndCheckResult( matcherA, mismatchContent );

        // 全文检索条件匹配0文件，文件属性匹配全部文件
        BSONObject matcherD = new BasicBSONObject();
        BSONObject valueD = new BasicBSONObject();
        valueD.put( "$gte", 1024 );
        matcherD.put( "major_version", valueD );
        searchFileAndCheckResult( matcherD, mismatchContent );
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

    private void updateFilesContent( String filePath ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( filePath );
        file.setMimeType( MimeType.PLAIN );
    }

    private void searchFileAndCheckResult( BSONObject matcher, String match )
            throws ScmInvalidArgumentException, ScmException {
        ScmCursor< ScmFulltextSearchResult > resultA = ScmFactory.Fulltext
                .simpleSeracher( ws ).match( match ).fileCondition( matcher )
                .scope( ScopeType.SCOPE_ALL ).search();
        Assert.assertFalse( resultA.hasNext() );
    }
}
