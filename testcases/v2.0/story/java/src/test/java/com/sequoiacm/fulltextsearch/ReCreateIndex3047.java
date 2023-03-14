package com.sequoiacm.fulltextsearch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description SCM-3047:文件不存在或不符合条件，指定文件重新建索引
 * @author wuyan
 * @Date 2020.09.21
 * @version 1.00
 */

public class ReCreateIndex3047 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file3047";
    private String wsName = null;

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", "testauthor3047" );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );

    }

    @Test
    private void test() throws Exception {
        // 文件不存在，指定文件重建索引
        try {
            ScmId noExistFileId = new ScmId( "5f6477484000030077d713f2" );
            ScmFactory.Fulltext.rebuildFileIndex( ws, noExistFileId );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                Assert.fail( e.getMessage() + "; e=" + e.getErrorCode() + ":"
                        + e.getErrorType() );
            }
        }

        String filePath = TestTools.LocalFile.getRandomFile();
        fileId = ScmFileUtils.create( ws, fileName, filePath );
        try {
            ScmFactory.Fulltext.rebuildFileIndex( ws, fileId );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_MEET_WORKSPACE_INDEX_MATCHER ) {
                Assert.fail( e.getMessage() + "; e=" + e.getErrorCode() + ":"
                        + e.getErrorType() );
            }
        }
        ScmFileFulltextStatus actStatus = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId ).getStatus();
        Assert.assertEquals( actStatus, ScmFileFulltextStatus.NONE );
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
}
