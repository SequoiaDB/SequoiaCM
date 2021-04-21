package com.sequoiacm.fulltextsearch.concurrent;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
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
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-3008 :: 并发更新文件不同属性
 * @author wuyan
 * @Date 2020.09.24
 * @version 1.00
 */

public class UpdateFileAttr3008 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file3008";
    private String matchCond = "matchcreateindex3008";
    private String noMatchCond = "nomatchindex3008";
    private String wsName = null;
    private BSONObject matcher = new BasicBSONObject();

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject value = new BasicBSONObject();
        value.put( "$lt", matchCond );
        matcher.put( "title", value );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );

        // 创建文件，文件属性不匹配ws创建索引的file_matcher条件
        String filePath = TestTools.LocalFile.getRandomFile();
        fileId = ScmFileUtils.create( ws, fileName, filePath );
    }

    @Test
    private void test() throws Exception {
        int createIndexNum = 30;
        ThreadExecutor threadExec = new ThreadExecutor();

        for ( int i = 0; i < createIndexNum; i++ ) {
            if ( i % 2 == 0 ) {
                String title = "matchcreateindex0_3008_" + i;
                threadExec.addWorker( new UpdateFileAttrThread( title ) );
            } else {
                String title = noMatchCond + "_" + i;
                threadExec.addWorker( new UpdateFileAttrThread( title ) );
            }
        }

        threadExec.run();

        ScmFileFulltextStatus actStatus = ScmFactory.Fulltext
                .getFileIndexInfo( ws, fileId ).getStatus();
        if ( actStatus.equals( ScmFileFulltextStatus.CREATED ) ) {
            FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                    matcher, matcher );
        } else {
            Assert.assertEquals( actStatus, ScmFileFulltextStatus.NONE );
        }
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

    private class UpdateFileAttrThread {
        String title;

        public UpdateFileAttrThread( String title ) {
            this.title = title;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setTitle( title );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
