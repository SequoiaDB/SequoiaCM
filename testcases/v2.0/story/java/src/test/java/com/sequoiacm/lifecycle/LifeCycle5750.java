package com.sequoiacm.lifecycle;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @descreption SCM-5750:查询全局Transition验证
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5750 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5750a";
    private String stageTagName2 = "testTag5750b";
    private String fowlName1 = "testTag5750a_testTag5750b";
    private String fowlName2 = "testTag5750b_testTag5750a";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // test a : 全局Transition数量为0
        lifeCycleConfig = prepareLifeCycleConfig1();
        LifeCycleUtils.cleanLifeCycleConfig( session );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );

        List< ScmLifeCycleTransition > actTransitions = ScmSystem.LifeCycleConfig
                .getTransitionConfig( session );
        List< ScmLifeCycleTransition > expTransitions = new ArrayList<>();
        Assert.assertEquals( actTransitions, expTransitions );

        // test b : 全局Transition数量为1
        lifeCycleConfig = prepareLifeCycleConfig2();
        LifeCycleUtils.cleanLifeCycleConfig( session );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );

        actTransitions = ScmSystem.LifeCycleConfig
                .getTransitionConfig( session );
        LifeCycleUtils.checkTransitionConfig( actTransitions,
                lifeCycleConfig.getTransitionConfig() );

        // test c : 全局Transition数量为多个
        lifeCycleConfig = prepareLifeCycleConfig3();
        LifeCycleUtils.cleanLifeCycleConfig( session );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
        actTransitions = ScmSystem.LifeCycleConfig
                .getTransitionConfig( session );
        LifeCycleUtils.checkTransitionConfig( actTransitions,
                lifeCycleConfig.getTransitionConfig() );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public ScmLifeCycleConfig prepareLifeCycleConfig1() throws ScmException {
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

        // 组装为lifeCycleConfig
        ScmLifeCycleConfig lifeCycleConfig = new ScmLifeCycleConfig();
        lifeCycleConfig.setStageTagConfig( stageTagConfig );
        return lifeCycleConfig;
    }

    public ScmLifeCycleConfig prepareLifeCycleConfig2() throws ScmException {
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
        triggers1.add( LifeCycleUtils.initTrigger( "1", "ALL", "30d", "10d",
                "1d", "1d", true ) );
        triggers1.add( LifeCycleUtils.initTrigger( "2", "ANY", "10d", "0d",
                "1d", "1d", true ) );

        ScmTransitionTriggers transitionTriggers = LifeCycleUtils
                .initScmTransitionTriggers( "* * * * * ?", "ANY", 300000,
                        triggers1 );

        // 延迟清理触发器配置
        List< ScmTrigger > triggers2 = new ArrayList<>();
        triggers2.add( LifeCycleUtils.initTrigger( "1", "ALL", "30d", "10d",
                "1d", "1d", false ) );
        triggers2.add( LifeCycleUtils.initTrigger( "2", "ANY", "10d", "0d",
                "1d", "1d", false ) );
        ScmCleanTriggers cleanTriggers = LifeCycleUtils.initScmCleanTriggers(
                "* * * * * ?", "ANY", 300000, triggers2 );

        // Transition配置
        ScmLifeCycleTransition scmLifeCycleTransition = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName1, stageTagName1,
                        stageTagName2, transitionTriggers, cleanTriggers,
                        new BasicBSONObject(), "strict", false, true, "ALL" );

        List< ScmLifeCycleTransition > transitionConfig = new ArrayList<>();
        transitionConfig.add( scmLifeCycleTransition );

        // 组装为lifeCycleConfig
        ScmLifeCycleConfig lifeCycleConfig = new ScmLifeCycleConfig();
        lifeCycleConfig.setStageTagConfig( stageTagConfig );
        lifeCycleConfig.setTransitionConfig( transitionConfig );
        return lifeCycleConfig;
    }

    public ScmLifeCycleConfig prepareLifeCycleConfig3() throws ScmException {
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
        triggers1.add( LifeCycleUtils.initTrigger( "1", "ALL", "30d", "10d",
                "1d", "1d", true ) );
        triggers1.add( LifeCycleUtils.initTrigger( "2", "ANY", "10d", "0d",
                "1d", "1d", true ) );
        ScmTransitionTriggers transitionTriggers = LifeCycleUtils
                .initScmTransitionTriggers( "* * * * * ?", "ANY", 300000,
                        triggers1 );

        // 延迟清理触发器配置
        List< ScmTrigger > triggers2 = new ArrayList<>();
        triggers2.add( LifeCycleUtils.initTrigger( "1", "ALL", "30d", "10d",
                "1d", "1d", false ) );
        triggers2.add( LifeCycleUtils.initTrigger( "2", "ANY", "10d", "0d",
                "1d", "1d", false ) );
        ScmCleanTriggers cleanTriggers = LifeCycleUtils.initScmCleanTriggers(
                "* * * * * ?", "ANY", 300000, triggers2 );

        // Transition配置
        ScmLifeCycleTransition scmLifeCycleTransition1 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName1, stageTagName1,
                        stageTagName2, transitionTriggers, cleanTriggers,
                        new BasicBSONObject(), "strict", false, true, "ALL" );

        ScmLifeCycleTransition scmLifeCycleTransition2 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName2, stageTagName2,
                        stageTagName1, transitionTriggers, cleanTriggers,
                        new BasicBSONObject(), "strict", false, true, "ALL" );

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