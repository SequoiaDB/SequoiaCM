package com.sequoiacm.fulltextsearch.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
 * @Description SCM-3000:并发ws更新索引和新建文件
 * @author wuyan
 * @Date 2020.09.23
 * @version 1.00
 */

public class CreateFileAndWsAlterIndex3000 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3000";
    private String wsName = null;
    private String oldMatchCond = "测试file3000";
    private String newMatchCond = "newtestfile3000";
    private List< ScmId > fileIds = new ArrayList< >();
    private MultiValueMap< String, ScmId > fileInfoMap = new LinkedMultiValueMap< String, ScmId >();

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

        // 创建文件，文件属性匹配ws创建索引的file_matcher条件
        for ( int i = 0; i < 5; i++ ) {
            String subFileName = fileName + "_" + i;
            ScmId fileId = createFile( ws, subFileName, oldMatchCond );
            fileIds.add( fileId );
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        WsAlterIndexThread wsAlterIndex = new WsAlterIndexThread();
        CreateFileThread createfileCreateIndex = new CreateFileThread(
                oldMatchCond, oldMatchCond );
        CreateFileThread createfileNoIndex = new CreateFileThread( newMatchCond,
                newMatchCond );
        threadExec.addWorker( wsAlterIndex );
        threadExec.addWorker( createfileCreateIndex );
        threadExec.addWorker( createfileNoIndex );

        threadExec.run();

        // 检查ws索引状态和索引条件
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        String updateCondValue = ( String ) ScmFactory.Fulltext
                .getIndexInfo( ws ).getFileMatcher().get( "title" );
        Assert.assertEquals( updateCondValue, newMatchCond );

        ScmFactory.Fulltext.inspectIndex( ws );
        // 检查文件索引状态
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                fileInfoMap.get( newMatchCond ).get( 0 ) );
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                fileInfoMap.get( oldMatchCond ).get( 0 ) );
        int expNoCreateIndexCount = 25;
        int expCreateIndexCount = 20;
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.NONE,
                expNoCreateIndexCount );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                expCreateIndexCount );
        // 检索文件
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "title", newMatchCond );
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                matcher, matcher );
        runSuccess = true;

    }

    @AfterClass
    private void tearDown() throws Exception {

        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileIds.size(); i++ ) {
                    ScmId fileId = fileIds.get( i );
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

    private class CreateFileThread {
        String fileName;
        String title;

        public CreateFileThread( String fileName, String title ) {
            this.fileName = fileName;
            this.title = title;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                // 新增20个文件
                for ( int i = 0; i < 20; i++ ) {
                    String name = fileName + "_" + i;
                    ScmId fileId = createFile( ws, name, title );
                    fileInfoMap.add( title, fileId );
                    fileIds.add( fileId );
                }

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
