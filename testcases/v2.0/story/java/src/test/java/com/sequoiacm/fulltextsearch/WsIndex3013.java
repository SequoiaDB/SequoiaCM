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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description SCM-3013:无存量数据，索引模式为同步，工作区创建索引后，增删改文件
 * @author wuyan
 * @Date 2020.09.17
 * @version 1.00
 */

public class WsIndex3013 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String author = "author3013";
    private String newAuthor = "author3013_NEW";
    private List< ScmId > fileIdList = new ArrayList< >();
    private String wsName = "ws3013";

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test
    private void test() throws Exception {
        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", author );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        String fileNamePrefix = "file3013_";
        int fileNum = 4;
        String filePath = TestTools.LocalFile.getRandomFile();
        fileIdList = createFiles( fileNamePrefix, author, filePath, fileNum );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                fileNum );
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                matcher, matcher );

        // 更新文件创建索引
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.updateContent( filePath );
        }
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                fileNum * 2 );
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_ALL,
                new BasicBSONObject(), new BasicBSONObject() );

        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            if ( i % 2 == 0 ) {
                // 更新字段不匹配ws的file_matcher条件字段
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setAuthor( newAuthor );
            } else {
                // 删除文件
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        }

        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 0 );
        ScmCursor< ScmFulltextSearchResult > result1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .notMatch( "condition" ).scope( ScmType.ScopeType.SCOPE_ALL )
                .search();
        Assert.assertFalse( result1.hasNext() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileIdList.size(); i++ ) {
                    ScmId fileId = fileIdList.get( i );
                    try {
                        ScmFactory.File.deleteInstance( ws, fileId, true );
                    } catch ( ScmException e ) {
                        Assert.assertEquals( e.getError(),
                                ScmError.FILE_NOT_FOUND, e.getMessage() );
                    }

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

    private List< ScmId > createFiles( String fileNamePrefix, String author,
            String filePath, int fileNum ) throws Exception {
        List< ScmId > fileIdList = new ArrayList< >();
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNamePrefix + "_" + i );
            file.setAuthor( author );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
        return fileIdList;
    }
}
