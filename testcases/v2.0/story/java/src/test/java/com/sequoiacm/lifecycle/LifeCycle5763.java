package com.sequoiacm.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5763:根据Transition获取工作区列表
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5763 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5763_1";
    private String stageTagName2 = "testTag5763_2";
    private String fowlName1 = "testFowlName5763_1To2";
    private String fowlName2 = "testFowlName5763_2To1";
    private String fileName = "file5763";
    private String wsNameA = "ws5763a";
    private String wsNameB = "ws5763b";
    private BSONObject queryCond = null;
    private String cornRule = "0/1 * * * * ?";

    @BeforeClass
    private void setUp() throws Exception {
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();

        site = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsNameA, session );
        ScmWorkspaceUtil.deleteWs( wsNameB, session );

        LifeCycleUtils.cleanLifeCycleConfig( session );
        lifeCycleConfig = prepareLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName2 );
        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );

        ScmWorkspaceUtil.createWS( session, wsNameA, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.createWS( session, wsNameB, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsNameA );
        ScmWorkspaceUtil.wsSetPriority( session, wsNameB );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 多个工作区设置Transition
        ScmWorkspace wsA = ScmFactory.Workspace.getWorkspace( wsNameA,
                session );
        wsA.applyTransition( fowlName1 );
        ScmWorkspace wsB = ScmFactory.Workspace.getWorkspace( wsNameB,
                session );
        wsB.applyTransition( fowlName1 );

        List< ScmTransitionApplyInfo > transitionApplyInfos = ScmSystem.LifeCycleConfig
                .listWorkspace( session, fowlName1 );
        for ( ScmTransitionApplyInfo transition : transitionApplyInfos ) {
            String actWsName = transition.getWorkspace();
            if (!Objects.equals(actWsName, wsNameA)) {
                if (!Objects.equals(actWsName, wsNameB)) {
                    Assert.fail(
                            "返回值存在非预期工作区，工作区名为：" + actWsName );
                }
            }
        }

        LifeCycleUtils.cleanWsLifeCycleConfig( wsA );
        LifeCycleUtils.cleanWsLifeCycleConfig( wsB );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            ScmWorkspaceUtil.deleteWs( wsNameA, session );
            ScmWorkspaceUtil.deleteWs( wsNameB, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public ScmLifeCycleConfig prepareLifeCycleConfig() {
        // 阶段标签配置
        List< ScmLifeCycleStageTag > stageTagConfig = new ArrayList<>();
        ScmLifeCycleStageTag testStageTag1 = new ScmLifeCycleStageTag();
        testStageTag1.setName( stageTagName1 );
        testStageTag1.setDesc( stageTagName1 );
        ScmLifeCycleStageTag testStageTag2 = new ScmLifeCycleStageTag();
        testStageTag2.setName( stageTagName2 );
        testStageTag2.setDesc( stageTagName2 );

        stageTagConfig.add( testStageTag1 );
        stageTagConfig.add( testStageTag2 );

        // 文件流转触发器配置
        List< ScmTrigger > triggers1 = new ArrayList<>();
        triggers1.add( LifeCycleUtils.initTrigger( "1", "ALL", "0d", "0d", "0d",
                "0d", true ) );
        triggers1.add( LifeCycleUtils.initTrigger( "2", "ANY", "0d", "0d", "0d",
                "0d", true ) );
        ScmTransitionTriggers transitionTriggers = LifeCycleUtils
                .initScmTransitionTriggers( cornRule, "ANY", 300000,
                        triggers1 );

        // Transition配置
        ScmLifeCycleTransition scmLifeCycleTransition1 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName1, stageTagName1,
                        stageTagName2, transitionTriggers, null, queryCond,
                        "strict", false, true, "CURRENT" );

        ScmLifeCycleTransition scmLifeCycleTransition2 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName2, stageTagName2,
                        stageTagName1, transitionTriggers, null, queryCond,
                        "strict", false, true, "CURRENT" );

        List< ScmLifeCycleTransition > transitionConfig = new ArrayList<>();
        transitionConfig.add( scmLifeCycleTransition1 );
        transitionConfig.add( scmLifeCycleTransition2 );

        // 组装为lifeCycleConfig
        ScmLifeCycleConfig lifeCycleConfig = new ScmLifeCycleConfig();
        lifeCycleConfig.setStageTagConfig( stageTagConfig );
        lifeCycleConfig.setTransitionConfig( transitionConfig );
        return lifeCycleConfig;
    }
}