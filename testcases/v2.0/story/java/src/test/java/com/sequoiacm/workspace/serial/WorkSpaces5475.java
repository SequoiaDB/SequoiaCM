package com.sequoiacm.workspace.serial;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.exception.ScmError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5475:指定站点，多个线程并发修改数据源分区规则
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5475 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws5475";
    private ArrayList< SiteWrapper > siteList1 = new ArrayList<>();
    private ArrayList< SiteWrapper > siteList2 = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( rootSite );
        siteList1.add( rootSite );
        siteList2.add( branSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new UpdateWsShardingTypeThread( ScmShardingType.YEAR,
                rootSite, siteList1 ) );
        t.addWorker( new UpdateWsShardingTypeThread( ScmShardingType.MONTH,
                branSite, siteList2 ) );
        t.run();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspaceUtil.deleteWs( wsName, session );
            }
        } finally {
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
            ScmSession session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                        .prepareWsDataLocation( siteList, wsShardingType );
                ws.updateDataLocation( dataLocation );
                ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_CACHE_EXPIRE ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }
}