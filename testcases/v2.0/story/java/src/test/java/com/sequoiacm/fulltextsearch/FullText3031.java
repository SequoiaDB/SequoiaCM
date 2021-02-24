package com.sequoiacm.fulltextsearch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.common.MimeType;
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
 * @Description: SCM-3031 :: 全文检索条件匹配的文件和文件属性条件匹配的文件有交集，全文检索
 * @author fanyu
 * @Date:2020/11/16
 * @version:1.0
 */
public class FullText3031 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3031_";
    private String rootDirId = null;
    private String dirId = null;
    private String dirName = "/dir3031";
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private List< ScmId > fileIdList3 = new ArrayList<>();
    private BSONObject fileCondition = null;
    private int fileNum = 60;
    private File localPath = null;
    private String content1 = "text ";
    private String content2 = "test ";
    private String filePath1 = null;
    private String filePath2 = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath1 = localPath + File.separator + "localFile_" + 1024 + ".txt";
        TestTools.LocalFile.createFile( filePath1, content1, 1024 );
        filePath2 = localPath + File.separator + "localFile_" + 200 * 1024
                + ".txt";
        TestTools.LocalFile.createFile( filePath2, content2, 200 * 1024 );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        // 获取根目录id
        rootDirId = ScmFactory.Directory.getInstance( ws, "/" ).getId();
        dirId = ScmFactory.Directory.createInstance( ws, dirName ).getId();
        // 准备文件
        prepareFile( fileNameBase, fileNum );
        // 创建索引
        fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                .get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        // 全文检索条件匹配所有文件，文件属性条件匹配所有文件
        ScmCursor< ScmFulltextSearchResult > actFulltextResults1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).notMatch( "condition" )
                .search();
        List< String > fileIdStrList1 = new ArrayList<>();
        for ( ScmId fileId : fileIdList1 ) {
            fileIdStrList1.add( fileId.get() );
        }
        BSONObject expCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdStrList1 )
                .get();
        ScmCursor< ScmFileBasicInfo > expNormalResult1 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_ALL, expCond );
        FullTextUtils.checkCursor( actFulltextResults1, expNormalResult1 );

        // 全文检索条件匹配部分文件，文件属性条件匹配所有文件
        ScmCursor< ScmFulltextSearchResult > actFulltextResults2 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_CURRENT ).match( content2 )
                .search();
        ScmCursor< ScmFileBasicInfo > expNormalResult2 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_CURRENT, expCond );
        FullTextUtils.checkCursor( actFulltextResults2, expNormalResult2 );

        // 全文检索条件匹配部分文件，文件属性条件匹配部分文件
        List< String > fileIdStrList2 = new ArrayList<>();
        fileIdStrList2.addAll( fileIdStrList1 );
        fileIdStrList2.add( fileIdList2.get( 0 ).get() );
        fileIdStrList2.add( fileIdList3.get( 0 ).get() );
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdStrList2 )
                .get();
        ScmCursor< ScmFulltextSearchResult > actFulltextResults3 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( fileCondition )
                .scope( ScmType.ScopeType.SCOPE_HISTORY ).match( content1 )
                .search();
        ScmCursor< ScmFileBasicInfo > expNormalResult3 = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_HISTORY, expCond );
        FullTextUtils.checkCursor( actFulltextResults3, expNormalResult3 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId fileId : fileIdList3 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                ScmFactory.Directory.deleteInstance( ws, dirName );
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

    private void prepareFile( String fileNameBase, int fileNum )
            throws Exception {
        // 支持的文件类型
        for ( int i = 0; i < fileNum / 3; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setContent( filePath1 );
            file.setDirectory( rootDirId );
            fileIdList1.add( file.save() );
            file.updateContent( filePath2 );
        }

        // 不支持的文件类型
        for ( int i = fileNum / 3; i < 2 * fileNum / 3; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.ENVOY );
            file.setContent( filePath1 );
            file.setDirectory( rootDirId );
            fileIdList2.add( file.save() );
            file.updateContent( filePath2 );
        }

        // 不符合建索引文件
        for ( int i = fileNum / 3; i < 2 * fileNum / 3; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setContent( filePath2 );
            file.setDirectory( dirId );
            fileIdList3.add( file.save() );
            file.updateContent( filePath2 );
        }
    }
}