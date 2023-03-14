package com.sequoiacm.workspace;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5474:指定工作区，多个线程并发修改数据源分区规则
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5474 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsNameA = "ws5474a";
    private String wsNameB = "ws5474B";
    private List< SiteWrapper > siteList = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        siteList.add( site );
        ScmWorkspaceUtil.deleteWs( wsNameA, session );
        ScmWorkspaceUtil.deleteWs( wsNameB, session );
        ScmWorkspaceUtil.createWS( session, wsNameA, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.createWS( session, wsNameB, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsNameA );
        ScmWorkspaceUtil.wsSetPriority( session, wsNameB );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new UpdateWsShardingTypeThread( ScmShardingType.YEAR,
                wsNameA ) );
        t.addWorker( new UpdateWsShardingTypeThread( ScmShardingType.MONTH,
                wsNameB ) );
        t.run();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            ScmWorkspaceUtil.deleteWs( wsNameA, session );
            ScmWorkspaceUtil.deleteWs( wsNameB, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class UpdateWsShardingTypeThread {
        private ScmShardingType wsShardingType;
        private String wsName;

        public UpdateWsShardingTypeThread( ScmShardingType wsShardingType,
                String wsName ) {
            this.wsShardingType = wsShardingType;
            this.wsName = wsName;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = ScmSessionUtils.createSession( site );
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
}