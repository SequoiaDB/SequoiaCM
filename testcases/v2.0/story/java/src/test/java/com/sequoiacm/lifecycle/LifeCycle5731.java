package com.sequoiacm.lifecycle;

import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5731:设置生命周期全局配置与站点阶段标签信息验证
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5731 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private static String defaultHot = "Hot";
    private static String defaultWarm = "Warm";
    private static String defaultCold = "Cold";

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        LifeCycleUtils.cleanLifeCycleConfig( session );
    }

    @Test
    public void test() throws ScmException {
        // 站点标签与内置完全相同
        ScmLifeCycleConfig config = createScmLifeCycleTransitions( defaultHot,
                defaultWarm, defaultCold );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        ScmLifeCycleConfig actConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( actConfig, config );

        // 站点标签与内置部分相同
        ScmSystem.LifeCycleConfig.deleteLifeCycleConfig( session );
        config = createScmLifeCycleTransitions( defaultHot, defaultWarm,
                "test_cold" );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        actConfig = ScmSystem.LifeCycleConfig.getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( actConfig, config );

        // 站点标签与内置的全部不同
        ScmSystem.LifeCycleConfig.deleteLifeCycleConfig( session );
        config = createScmLifeCycleTransitions( "test_hot", "test_warm",
                "test_cold" );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        actConfig = ScmSystem.LifeCycleConfig.getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( actConfig, config );
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }

    private static ScmLifeCycleConfig createScmLifeCycleTransitions(
            String siteOneTag, String siteTwoTag, String siteThreeTag ) {
        ScmLifeCycleConfig config = new ScmLifeCycleConfig();
        List< ScmLifeCycleStageTag > scmLifeCycleStageTags = LifeCycleUtils
                .buildScmLifeCycleStageTags(
                        new ScmLifeCycleStageTag( siteOneTag, siteOneTag ),
                        new ScmLifeCycleStageTag( siteTwoTag, siteTwoTag ),
                        new ScmLifeCycleStageTag( siteThreeTag,
                                siteThreeTag ) );
        config.setStageTagConfig( scmLifeCycleStageTags );
        ScmLifeCycleTransition SiteOne_Site_two = LifeCycleUtils
                .initScmLifeCycleTransition( "SiteOne_SiteTwo", siteOneTag,
                        siteTwoTag, new BasicBSONObject() );
        ScmLifeCycleTransition SiteTwo_SiteThree = LifeCycleUtils
                .initScmLifeCycleTransition( "SiteTwo_SiteThree", siteTwoTag,
                        siteThreeTag, new BasicBSONObject() );
        List< ScmLifeCycleTransition > scmLifeCycleTransitions = LifeCycleUtils
                .buildScmLifeCycleTransitions( SiteOne_Site_two,
                        SiteTwo_SiteThree );
        config.setTransitionConfig( scmLifeCycleTransitions );
        return config;
    }
}