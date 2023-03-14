package com.sequoiacm.lifecycle;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5714:站点删除阶段标签
 * @author YiPan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5714 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagWarm = LifeCycleUtils.tagWarm.getName();
    private ScmLifeCycleConfig config;
    private WsWrapper wsp;
    private ScmWorkspace ws;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = ScmSessionUtils.createSession( rootSite );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws ScmException {
        // 未设置站点标签
        ScmFactory.Site.unsetSiteStageTag( session, rootSite.getSiteName() );
        String actTag = ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() );
        Assert.assertEquals( actTag, "" );

        // 设置站点标签，未被使用删除
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                tagHot );
        ScmFactory.Site.unsetSiteStageTag( session, rootSite.getSiteName() );
        actTag = ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() );
        Assert.assertEquals( actTag, "" );

        // 设置站点标签且被使用删除
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                tagHot );
        ScmFactory.Site.setSiteStageTag( session, branchSite.getSiteName(),
                tagWarm );
        ws.applyTransition( LifeCycleUtils.hotWarmName );
        try {
            ScmFactory.Site.unsetSiteStageTag( session,
                    rootSite.getSiteName() );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }
        actTag = ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() );
        Assert.assertEquals( actTag, tagHot );

        // 移除绑定数据流后删除
        ws.removeTransition( LifeCycleUtils.hotWarmName );
        ScmFactory.Site.unsetSiteStageTag( session, rootSite.getSiteName() );
        ScmFactory.Site.unsetSiteStageTag( session, branchSite.getSiteName() );
        Assert.assertEquals( ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() ), "" );
        Assert.assertEquals( ScmFactory.Site.getSiteStageTag( session,
                branchSite.getSiteName() ), "" );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanWsLifeCycleConfig( ws );
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }
}