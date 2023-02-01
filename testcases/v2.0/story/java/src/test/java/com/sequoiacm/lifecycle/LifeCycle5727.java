package com.sequoiacm.lifecycle;

import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5727:设置生命周期全局配置验证
 * @author YiPan
 * @date 2023/1/17
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5727 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private WsWrapper wsp;
    private ScmWorkspace ws;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagWarm = LifeCycleUtils.tagWarm.getName();
    private String how_warm_name = "newHot_newWarm";
    private String warm_cold_name = "newWarm_NewCold";
    private ScmLifeCycleConfig config = new ScmLifeCycleConfig();
    private ScmLifeCycleConfig updateConfig = new ScmLifeCycleConfig();

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( rootSite );
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        createLifeCycleConfig();
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws ScmException {
        // 没有站点和工作区使用全局配置
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        ScmLifeCycleConfig lifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( lifeCycleConfig, config );

        // 有站点使用阶段标签
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                tagHot );
        try {
            ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                    updateConfig );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 有工作区使用流
        ScmFactory.Site.setSiteStageTag( session, branchSite.getSiteName(),
                tagWarm );
        ws.applyTransition( how_warm_name );
        try {
            ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                    updateConfig );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }
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

    private void createLifeCycleConfig() {
        // 构造热、温、冷三个标签
        List< ScmLifeCycleStageTag > scmLifeCycleStageTags = LifeCycleUtils
                .initScmLifeCycleStageTags();
        // 构造三个数据流
        ScmLifeCycleTransition hot_warm = LifeCycleUtils
                .initScmLifeCycleTransition( how_warm_name,
                        LifeCycleUtils.tagHot.getName(),
                        LifeCycleUtils.tagWarm.getName(),
                        new BasicBSONObject() );
        ScmLifeCycleTransition warm_cold = LifeCycleUtils
                .initScmLifeCycleTransition( warm_cold_name,
                        LifeCycleUtils.tagWarm.getName(),
                        LifeCycleUtils.tagCold.getName(),
                        new BasicBSONObject() );
        // 创建全局配置
        config.setStageTagConfig( scmLifeCycleStageTags );
        config.setTransitionConfig(
                LifeCycleUtils.buildScmLifeCycleTransitions( hot_warm ) );
        // 创建更新配置
        updateConfig.setStageTagConfig( scmLifeCycleStageTags );
        updateConfig.setTransitionConfig(
                LifeCycleUtils.buildScmLifeCycleTransitions( warm_cold ) );
    }
}