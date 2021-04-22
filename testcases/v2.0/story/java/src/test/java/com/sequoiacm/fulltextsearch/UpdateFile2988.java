package com.sequoiacm.fulltextsearch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.common.MimeType;
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
 * @Description SCM-2988:多次更新文件，部分版本文件创建索引
 * @author wuyan
 * @Date 2020.09.14
 * @version 1.00
 */

public class UpdateFile2988 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file2988";
    private String wsName = null;
    private BSONObject matcher = new BasicBSONObject();

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject value = new BasicBSONObject();
        value.put( "$gt", 2 );
        matcher.put( "major_version", value );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );

        String filePath = TestTools.LocalFile.getFileByType( FileType.DOCX );
        fileId = ScmFileUtils.create( ws, fileName, filePath );
    }

    @Test
    private void test() throws Exception {
        int updateNum = 10;
        for ( int i = 0; i < updateNum; i++ ) {
            updateFileContent();
        }

        int expCount = updateNum + 1;
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                expCount );
        // 全文检索
        BSONObject matcher = new BasicBSONObject();
        BSONObject value = new BasicBSONObject();
        value.put( "$gte", 0 );
        matcher.put( "major_version", value );
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_ALL, matcher,
                new BasicBSONObject() );
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
