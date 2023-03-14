package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5478:修改数据源分区规则和文件删除并发
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5478 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws5478";
    private String fileName = "file5478";
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private ScmId scmId = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
        siteList.add( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        scmId = file.save();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new UpdateWsShardingTypeThread( ScmShardingType.YEAR, site,
                siteList ) );
        t.addWorker( new UpdateFileThread( site ) );
        t.run();

        try {
            ScmFactory.File.getInstance( ws, scmId );
            Assert.fail( "预期失败实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class UpdateWsShardingTypeThread {
        private ScmShardingType wsShardingType;
        private SiteWrapper site;
        private ArrayList< SiteWrapper > siteList;

        public UpdateWsShardingTypeThread( ScmShardingType wsShardingType,
                SiteWrapper site, ArrayList< SiteWrapper > siteList ) {
            this.wsShardingType = wsShardingType;
            this.site = site;
            this.siteList = siteList;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = ScmSessionUtils
                    .createSession( WorkSpaces5478.this.site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                        .prepareWsDataLocation( siteList, wsShardingType );
                ws.updateDataLocation( dataLocation );
                ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );
            } finally {
                session.close();
            }
        }
    }

    private class UpdateFileThread {
        private SiteWrapper site;

        public UpdateFileThread( SiteWrapper site ) {
            this.site = site;

        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                ScmFile file = ScmFactory.File.getInstance( ws, scmId );
                file.delete( true );
            } finally {
                session.close();
            }
        }
    }
}