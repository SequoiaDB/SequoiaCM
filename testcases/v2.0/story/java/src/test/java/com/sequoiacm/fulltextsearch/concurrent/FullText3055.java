
package com.sequoiacm.fulltextsearch.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3055 :: 并发相同工作区删除索引
 * @author fanyu
 * @Date:2020/11/18
 * @version:1.0
 */
public class FullText3055 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String fileNameBase = "file3055-";
    private int fileNum = 20;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.TEXT );
        prepareFile();
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Throwable {
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new DropIndex() );
        threadExec.addWorker( new DropIndex() );
        threadExec.run();

        // 等待索引建立
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );

        // 检查结果
        checkResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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

    private void checkResult() throws ScmException {
        // 获取工作区索引信息
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertNull( indexInfo.getMode() );
        Assert.assertNull( indexInfo.getFulltextLocation() );
        Assert.assertNull( indexInfo.getFileMatcher() );
        Assert.assertNull( indexInfo.getJobInfo() );
        Assert.assertEquals( indexInfo.getStatus(), ScmFulltextStatus.NONE );

        BSONObject condition = ScmQueryBuilder
                .start( "external_data.fulltext_status" ).is( "NONE" ).get();
        long actCount = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        Assert.assertEquals( actCount, fileNum );

        // 全文检索
        try {
            ScmFactory.Fulltext.simpleSeracher( ws ).match( "condition" )
                    .fileCondition( new BasicBSONObject() ).search();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_DISABLE ) {
                throw e;
            }
        }
    }

    private void prepareFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setContent( filePath );
            fileIdList.add( file.save() );
        }
    }

    private class DropIndex {
        @ExecuteOrder(step = 1)
        private void drop() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.dropIndex( ws );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FULL_TEXT_INDEX_IS_DELETING
                        && e.getError() != ScmError.FULL_TEXT_INDEX_DISABLE ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
