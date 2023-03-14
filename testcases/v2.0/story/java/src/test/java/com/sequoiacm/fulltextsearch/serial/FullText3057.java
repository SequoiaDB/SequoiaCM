package com.sequoiacm.fulltextsearch.serial;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3057 :: 并发工作区删除索引和删除工作区
 * @author fanyu
 * @Date:2020/11/17
 * @version:1.0
 */
public class FullText3057 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws3057";
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String fileNameBase = "file3057-";
    private int fileNum = 10;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        prepareFile();
        // 创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Throwable {
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new DropIndex() );
        threadExec.addWorker( new DropWs() );
        threadExec.run();
        try {
            ScmFactory.Workspace.getWorkspace( wsName, session );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                throw e;
            }
        }
    }

    @AfterClass
    private void tearDown() throws Exception {
        if ( session != null ) {
            session.close();
        }
    }

    private void prepareFile() throws ScmException {
        byte[] bytes = new byte[ 1024 * 200 ];
        new Random().nextBytes( bytes );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.PLAIN );
            file.setContent( new ByteArrayInputStream( bytes ) );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private class DropIndex {

        @ExecuteOrder(step = 1)
        private void drop() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Fulltext.dropIndex( ws );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST
                        && e.getError() != ScmError.CONFIG_SERVER_ERROR ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DropWs {
        @ExecuteOrder(step = 1)
        private void drop() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
