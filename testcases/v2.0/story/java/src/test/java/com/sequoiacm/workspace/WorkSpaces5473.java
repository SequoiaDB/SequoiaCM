package com.sequoiacm.workspace;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.exception.ScmError;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5473:指定不同的修改方式，多个线程并发修改数据源分区规则
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5473 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws5473";
    private List< SiteWrapper > siteList = new ArrayList<>();
    private boolean runSuccess = false;
    private int threadUpdateMonthSuccess = 0;
    private int threadUpdateYearDateSuccess = 0;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        siteList.add( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker(
                new UpdateWsShardingTypeThread( ScmShardingType.YEAR, true ) );
        t.addWorker( new UpdateWsShardingTypeThread( ScmShardingType.MONTH,
                false ) );
        t.run();
        List< ScmDataLocation > dataLocationYear = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.YEAR );
        List< ScmDataLocation > dataLocationMonth = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.MONTH );

        if ( threadUpdateMonthSuccess == 0
                && threadUpdateYearDateSuccess == 1 ) {
            ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocationYear );
        } else if ( threadUpdateMonthSuccess == 1
                && threadUpdateYearDateSuccess == 0 ) {
            ScmWorkspaceUtil.checkWsUpdate( session, wsName,
                    dataLocationMonth );
        } else if ( threadUpdateMonthSuccess == 1
                && threadUpdateYearDateSuccess == 1 ) {
            // 所有线程均成功，校验两个线程结果，有一个为true则成功
            if ( !checkWsUpdate( session, wsName, dataLocationMonth )){
                if (!checkWsUpdate( session, wsName, dataLocationYear )){
                    throw new Exception("校验修改失败");
                }
            }
        } else {
            throw new Exception( "所有线程均失败" );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class UpdateWsShardingTypeThread {
        private ScmShardingType wsShardingType;
        private boolean updateMode;

        public UpdateWsShardingTypeThread( ScmShardingType wsShardingType,
                boolean updateMode ) {
            this.wsShardingType = wsShardingType;
            this.updateMode = updateMode;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                        .prepareWsDataLocation( siteList, wsShardingType );
                ws.updateDataLocation( dataLocation, updateMode );
                if ( wsShardingType == ScmShardingType.MONTH ) {
                    threadUpdateMonthSuccess = 1;
                } else {
                    threadUpdateYearDateSuccess = 1;
                }
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_CACHE_EXPIRE ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }

    public boolean checkWsUpdate( ScmSession session, String wsName,
            List< ScmDataLocation > expDataLocations ) throws ScmException {
        ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace( wsName,
                session );
        boolean updateStatus = false;
        List< ScmDataLocation > actDataLocations = workspace.getDataLocations();
        for ( ScmDataLocation actDataLocation : actDataLocations ) {
            for ( ScmDataLocation expDataLocation : expDataLocations ) {
                if ( actDataLocation.getSiteName()
                        .equals( expDataLocation.getSiteName() ) ) {
                    if ( actDataLocation.equals( expDataLocation ) ) {
                        updateStatus = true;
                    }
                }
            }
        }
        return updateStatus;
    }
}