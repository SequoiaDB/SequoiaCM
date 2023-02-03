package com.sequoiacm.lifecycle.concurrent;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5771:设置全局生命周期管理配置并发验证
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5771 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5771_1";
    private String stageTagName2 = "testTag5771_2";
    private String stageTagName3 = "testTag5771_3";
    private String fowlName1 = "testFowlName5771_1To2";
    private String fowlName2 = "testFowlName5771_2To1";
    private String fowlName3 = "testAddFowlName5771_2To1";
    private ScmLifeCycleTransition addTransition = null;
    private String fileName = "file5771";
    private ScmWorkspace ws = null;
    private BSONObject queryCond = null;
    private WsWrapper wsp = null;
    private String cornRule = "0/1 * * * * ?";

    @BeforeClass
    private void setUp() throws Exception {
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        wsp = ScmInfo.getWs();
        site = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();

        session = TestScmTools.createSession( rootSite );

        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        addTransition = prepareTransition( fowlName3 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // test a : 设置全局阶段标签
        test1();

        // test b : 删除全局阶段标签
        test2();

        // test c : 添加全局Transition
        test3();

        // test d : 删除全局Transition
        test4();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                LifeCycleUtils.cleanWsLifeCycleConfig( ws );
                LifeCycleUtils.cleanLifeCycleConfig( session );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void test1() throws Exception {
        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        lifeCycleConfig = prepareLifeCycleConfig();

        ThreadExecutor t = new ThreadExecutor();
        SetLifeCycleConfigThread setLifeCycleConfig = new SetLifeCycleConfigThread();
        AddSiteStageTagThread addSiteStageTag = new AddSiteStageTagThread();
        t.addWorker( setLifeCycleConfig );
        t.addWorker( addSiteStageTag );
        t.run();

        ScmLifeCycleConfig actLifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        List< ScmLifeCycleStageTag > stageTags = actLifeCycleConfig
                .getStageTagConfig();

        if ( setLifeCycleConfig
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode()
                && addSiteStageTag.getRetCode() == 0 ) {
            // 校验AddSiteStageTagThread线程成功场景
            boolean isStageTagsExit = false;
            for ( ScmLifeCycleStageTag stageTag : stageTags ) {
                if ( stageTag.getName().equals( stageTagName3 ) ) {
                    isStageTagsExit = true;
                }
            }
            if ( !isStageTagsExit ) {
                Assert.fail( "预期该标签应该存在，标签名为：" + stageTagName3 );
            }
        } else if ( setLifeCycleConfig.getRetCode() == 0 && addSiteStageTag
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
            // 校验SetLifeCycleConfigThread线程成功场景
            LifeCycleUtils.checkScmLifeCycleConfig( actLifeCycleConfig,
                    lifeCycleConfig );
        } else if ( setLifeCycleConfig.getRetCode() == 0
                && addSiteStageTag.getRetCode() == 0 ) {
            // 校验两个线程都成功场景
            List< ScmLifeCycleStageTag > expStageTag = lifeCycleConfig
                    .getStageTagConfig();
            expStageTag.add(
                    new ScmLifeCycleStageTag( stageTagName3, stageTagName3 ) );
            LifeCycleUtils.checkStageTagConfig( stageTags, expStageTag );
        } else {
            Assert.fail( "两个线程预期不该都失败！线程setLifeCycleConfig错误码为："
                    + setLifeCycleConfig.getRetCode()
                    + " ,线程addSiteStageTag错误码为；"
                    + addSiteStageTag.getRetCode() );
        }
    }

    public void test2() throws Exception {
        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        lifeCycleConfig = prepareLifeCycleConfig();

        ThreadExecutor t = new ThreadExecutor();
        SetLifeCycleConfigThread setLifeCycleConfig = new SetLifeCycleConfigThread();
        DeleteSiteStageTagThread deleteSiteStageTag = new DeleteSiteStageTagThread();
        t.addWorker( setLifeCycleConfig );
        t.addWorker( deleteSiteStageTag );
        t.run();

        ScmLifeCycleConfig actLifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );

        if ( setLifeCycleConfig.getRetCode() == 0 && deleteSiteStageTag
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
            // 校验setLifeCycleConfig线程成功场景
            LifeCycleUtils.checkScmLifeCycleConfig( actLifeCycleConfig,
                    lifeCycleConfig );
        } else if ( setLifeCycleConfig.getRetCode() == 0
                && deleteSiteStageTag.getRetCode() == 0 ) {
            // 校验两个线程都成功场景
            List< ScmLifeCycleStageTag > stageTags = actLifeCycleConfig
                    .getStageTagConfig();
            for ( ScmLifeCycleStageTag stageTag : stageTags ) {
                if ( stageTag.getName().equals( stageTagName2 ) ) {
                    Assert.fail( "校验结果失败，预期该stageTag应被删除" + stageTagName2 );
                }
            }
        } else {
            Assert.fail( "两个线程预期不该都失败！线程setLifeCycleConfig错误码为："
                    + setLifeCycleConfig.getRetCode()
                    + " ,线程deleteSiteStageTag错误码为；"
                    + deleteSiteStageTag.getRetCode() );
        }
    }

    public void test3() throws Exception {
        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        lifeCycleConfig = prepareLifeCycleConfig();

        ThreadExecutor t = new ThreadExecutor();
        SetLifeCycleConfigThread setLifeCycleConfig = new SetLifeCycleConfigThread();
        AddTransitionThread addTransitionThread = new AddTransitionThread();
        t.addWorker( setLifeCycleConfig );
        t.addWorker( addTransitionThread );
        t.run();

        ScmLifeCycleConfig actLifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );

        if ( setLifeCycleConfig.getRetCode() == 0 && addTransitionThread
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
            // 校验setLifeCycleConfig线程成功场景
            LifeCycleUtils.checkScmLifeCycleConfig( actLifeCycleConfig,
                    lifeCycleConfig );
        } else if ( setLifeCycleConfig.getRetCode() == 0
                && addTransitionThread.getRetCode() == 0 ) {
            // 校验两个线程都成功场景
            ScmLifeCycleTransition actTransition = ScmSystem.LifeCycleConfig
                    .getTransition( session, fowlName3 );
            Assert.assertEquals( actTransition.toBSONObject(),
                    addTransition.toBSONObject() );
        } else {
            Assert.fail( "两个线程预期不该都失败！线程setLifeCycleConfig错误码为："
                    + setLifeCycleConfig.getRetCode()
                    + " ,线程addTransitionThread错误码为；"
                    + addTransitionThread.getRetCode() );
        }
    }

    public void test4() throws Exception {
        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        lifeCycleConfig = prepareLifeCycleConfig();

        ThreadExecutor t = new ThreadExecutor();
        SetLifeCycleConfigThread setLifeCycleConfig = new SetLifeCycleConfigThread();
        RemoveTransitionThread removeTransition = new RemoveTransitionThread();
        t.addWorker( setLifeCycleConfig );
        t.addWorker( removeTransition );
        t.run();

        ScmLifeCycleConfig actLifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        if ( setLifeCycleConfig.getRetCode() == 0 && removeTransition
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
            // 校验setLifeCycleConfig线程成功场景
            LifeCycleUtils.checkScmLifeCycleConfig( actLifeCycleConfig,
                    lifeCycleConfig );
        } else if ( setLifeCycleConfig.getRetCode() == 0
                && removeTransition.getRetCode() == 0 ) {
            // 校验两个线程都成功场景
            try {
                ScmSystem.LifeCycleConfig.getTransition( session, fowlName1 );
                Assert.fail( "预期失败实际成功" );
            } catch ( ScmException e2 ) {
                if ( e2.getErrorCode() != ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
                    throw e2;
                }
            }
        } else {
            Assert.fail( "两个线程预期不该都失败！线程setLifeCycleConfig错误码为："
                    + setLifeCycleConfig.getRetCode()
                    + " ,线程removeTransition错误码为；"
                    + removeTransition.getRetCode() );
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

    public ScmLifeCycleTransition prepareTransition( String fowlName ) {
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
        ScmLifeCycleTransition scmLifeCycleTransition = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName, stageTagName1,
                        stageTagName2, transitionTriggers, null, queryCond,
                        "strict", false, true, "CURRENT" );

        return scmLifeCycleTransition;
    }

    private class SetLifeCycleConfigThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                        lifeCycleConfig );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }

    private class AddSiteStageTagThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmSystem.LifeCycleConfig.addStageTag( session, stageTagName3,
                        stageTagName3 );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }

    private class DeleteSiteStageTagThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmSystem.LifeCycleConfig.removeStageTag( session,
                        stageTagName2 );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }

    private class AddTransitionThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmSystem.LifeCycleConfig.addTransition( session,
                        addTransition );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }

    private class RemoveTransitionThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmSystem.LifeCycleConfig.removeTransition( session,
                        fowlName1 );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }
}