package com.sequoiacm.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmSessionUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5469:修改数据源分区规则验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5469 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper site = null;
    private String wsName = "ws5469";
    private ScmWorkspace rootSiteWs = null;
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        site = ScmInfo.getSite();
    }

    @DataProvider(name = "dataProvider")
    public Object[][] generateDate() {
        return new Object[][] { { ScmShardingType.YEAR },
                { ScmShardingType.QUARTER }, { ScmShardingType.MONTH },
                { ScmShardingType.DAY }, { ScmShardingType.NONE } };
    }

    @Test(groups = { "twoSite", "fourSite" }, dataProvider = "dataProvider")
    public void test( ScmShardingType wsShardingType ) throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsName, session );
        List< SiteWrapper > siteList = new ArrayList<>();
        siteList.add( site );
        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, wsShardingType );
        rootSiteWs.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );
        runSuccessCount.incrementAndGet();
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
}