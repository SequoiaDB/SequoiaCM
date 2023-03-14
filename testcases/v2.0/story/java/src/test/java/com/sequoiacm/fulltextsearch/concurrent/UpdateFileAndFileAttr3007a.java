package com.sequoiacm.fulltextsearch.concurrent;

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
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description SCM-3007 :: 并发创建新版本文件和更新文件属性
 * @author wuyan
 * @Date 2020.11.07
 * @version 1.00
 */

public class UpdateFileAndFileAttr3007a extends TestScmBase {

    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String filePath = null;
    private String fileName = "file3007a";
    private String wsName = null;
    private ScmId fileId = null;
    private BSONObject matcher = new BasicBSONObject();;

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject cond = new BasicBSONObject();
        cond.put( "$gte", fileName );
        matcher.put( "author", cond );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.async ) );

        // 创建文件，文件属性匹配ws创建索引的file_matcher条件
        filePath = TestTools.LocalFile.getRandomFile();
        fileId = ScmFileUtils.create( ws, fileName, filePath );

    }

    @Test
    private void test() throws Exception {
        UpdateFileAttrThread updateFileAttrThread = new UpdateFileAttrThread();
        UpdateFileThread updateFileThread = new UpdateFileThread();
        updateFileAttrThread.start();
        updateFileThread.start();

        Assert.assertTrue( updateFileThread.isSuccess(),
                updateFileThread.getErrorMsg() );
        Assert.assertTrue( updateFileAttrThread.isSuccess(),
                updateFileAttrThread.getErrorMsg() );
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                fileId );
        ScmCursor< ScmFulltextSearchResult > result = ScmFactory.Fulltext
                .simpleSeracher( ws ).notMatch( "condition" )
                .fileCondition( matcher ).search();
        while ( result.hasNext() ) {
            ScmFulltextSearchResult object = result.getNext();
            ScmFileBasicInfo fileinfo = object.getFileBasicInfo();
            System.out.println( "----3007afileinfo=" + fileinfo.toString() );
        }
        result.close();

        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_ALL,
                new BasicBSONObject(), new BasicBSONObject() );

    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
        } finally {
            if ( wsName != null ) {
                WsPool.release( wsName );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class UpdateFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setAuthor( fileName + "a" );
                file.updateContent( filePath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFileAttrThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setTitle( fileName );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
