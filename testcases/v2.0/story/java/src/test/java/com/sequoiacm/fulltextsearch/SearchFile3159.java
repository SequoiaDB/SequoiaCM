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
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.TestTools.LocalFile.FileType;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description SCM-3159 : 指定历史版本不存在的属性检索所有版本文件
 * @author wuyan
 * @Date 2020.09.14
 * @version 1.00
 */

public class SearchFile3159 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file3159";
    private String wsName = null;

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", fileName );

        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
    }

    @Test
    private void test() throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();
        fileId = ScmFileUtils.create( ws, fileName, filePath );
        updateFileContent();
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 2 );

        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", fileName );

        // 使用文件属性检索文件
        try {
            ScmCursor< ScmFulltextSearchResult > result = ScmFactory.Fulltext
                    .simpleSeracher( ws ).fileCondition( matcher )
                    .notMatch( "condition" )
                    .scope( ScmType.ScopeType.SCOPE_ALL ).search();

            result.close();
            Assert.fail( " seracher should be failed!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT,
                    e.getMessage() );
        }

        // 指定内容检索文件
        String matchContent = "SequoiaDB";
        ScmCursor< ScmFulltextSearchResult > result1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).fileCondition( matcher )
                .notMatch( matchContent ).search();

        Assert.assertFalse( result1.hasNext() );
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

    private void updateFileContent() throws Exception {
        String filePath = TestTools.LocalFile.getFileByType( FileType.DOCX );
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( filePath );
        file.setMimeType( MimeType.DOCX );
    }
}
