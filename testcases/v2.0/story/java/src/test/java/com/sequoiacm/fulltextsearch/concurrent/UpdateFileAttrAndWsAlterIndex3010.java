package com.sequoiacm.fulltextsearch.concurrent;

import java.util.HashMap;

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
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
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
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-3010 :: 并发ws更新索引和更新文件属性
 * @author wuyan
 * @Date 2020.09.23
 * @version 1.00
 */

public class UpdateFileAttrAndWsAlterIndex3010 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3010";
    private int fileNum = 15;
    private String wsName = null;
    private String oldMatchCond = "测试file3010";
    private String newMatchCond = "newtestfile3010";
    private HashMap< String, ScmId > fileInfoMap = new HashMap< >();

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", oldMatchCond );
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );

        // 创建文件，只有5个文件匹配ws创建索引的file_matcher条件
        for ( int i = 0; i < fileNum; i++ ) {
            String subFileName = fileName + "_" + i;
            if ( i < 5 ) {
                ScmId fileId = createFile( ws, subFileName, oldMatchCond );
                fileInfoMap.put( subFileName, fileId );
            } else {
                ScmId fileId = createFile( ws, subFileName, "noMatchIndex" );
                fileInfoMap.put( subFileName, fileId );
            }
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new WsAlterIndexThread() );
        for ( int i = 5; i < fileNum; i++ ) {
            // test b：5个文件，更新后属性匹配旧file_matcher条件
            if ( i < 10 ) {
                ScmId fileId = fileInfoMap.get( fileName + "_" + i );
                UpdateFileAttrThread updateFileAttr = new UpdateFileAttrThread(
                        fileId, oldMatchCond );
                threadExec.addWorker( updateFileAttr );
                // test a：5个文件，更新后属性匹配新file_matcher条件
            } else {
                ScmId fileId = fileInfoMap.get( fileName + "_" + i );
                UpdateFileAttrThread updateFileAttr = new UpdateFileAttrThread(
                        fileId, newMatchCond );
                threadExec.addWorker( updateFileAttr );
            }
        }

        threadExec.run();

        // 检查ws索引状态和索引条件
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        BSONObject filematcher = ScmFactory.Fulltext.getIndexInfo( ws )
                .getFileMatcher();
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", newMatchCond );
        Assert.assertEquals( filematcher, matcher );

        ScmFactory.Fulltext.inspectIndex( ws );
        for ( int i = 0; i < 15; i++ ) {
            if ( i < 10 ) {
                FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                        fileInfoMap.get( fileName + "_" + i ) );
            } else {
                FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                        fileInfoMap.get( fileName + "_" + i ) );
            }
        }

        // 检索文件
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                matcher, matcher );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileInfoMap.size(); i++ ) {
                    ScmId fileId = fileInfoMap.get( fileName + "_" + i );
                    ScmFactory.File.deleteInstance( ws, fileId, true );
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

    private class WsAlterIndexThread {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFulltextModifiler modifiler = new ScmFulltextModifiler();
                BSONObject matcher = new BasicBSONObject();
                matcher.put( "title", newMatchCond );
                modifiler.newFileCondition( matcher );
                ScmFactory.Fulltext.alterIndex( ws, modifiler );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFileAttrThread {
        ScmId fileId;
        String title;

        public UpdateFileAttrThread( ScmId fileId, String title ) {
            this.fileId = fileId;
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

    private ScmId createFile( ScmWorkspace ws, String fileName, String title )
            throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setTitle( title );
        file.setContent( filePath );
        ScmId fileId = file.save();
        return fileId;
    }
}
