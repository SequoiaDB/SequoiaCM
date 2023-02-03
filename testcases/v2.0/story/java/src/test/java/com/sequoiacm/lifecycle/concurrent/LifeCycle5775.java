package com.sequoiacm.lifecycle.concurrent;

import java.util.ArrayList;
import java.util.List;

import com.sequoiadb.threadexecutor.ResultStore;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5775:工作区配置Transition并发验证
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5775 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5775_1";
    private String stageTagName2 = "testTag5775_2";
    private String fowlName1 = "testFowlName5775_1To2";
    private String fowlName2 = "testFowlName5775_2To1";
    private String fowlName3 = "testAddFowlName5775a";

    private ScmLifeCycleTransition updateTransition = null;
    private String fileName = "file5775";
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

        updateTransition = prepareTransition( fowlName3 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // test a : 工作区配置Transition
        test1();
        // test b : 工作区删除Transition
        test2();
        // test c : 工作区更新Transition
        test3();
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
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        lifeCycleConfig = prepareLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName2 );

        ThreadExecutor t = new ThreadExecutor();
        AppyTransitionThread appyTransition1 = new AppyTransitionThread(
                fowlName1 );
        AppyTransitionThread appyTransition2 = new AppyTransitionThread(
                fowlName2 );
        t.addWorker( appyTransition1 );
        t.addWorker( appyTransition2 );
        t.run();

        if ( appyTransition1.getRetCode() == 0 && appyTransition2
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
            // 校验appyTransition1线程成功场景
            checkThreadTransition( fowlName1 );
        } else if ( appyTransition1
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode()
                && appyTransition2.getRetCode() == 0 ) {
            // 校验appyTransition2线程成功场景
            checkThreadTransition( fowlName2 );
        } else if ( appyTransition1.getRetCode() == 0
                && appyTransition2.getRetCode() == 0 ) {
            // 校验两个线程都成功场景
            checkThreadTransition( fowlName1 );
            checkThreadTransition( fowlName2 );
        } else {
            Assert.fail( "两个线程预期不该都失败！线程appyTransition1错误码为："
                    + appyTransition1.getRetCode() + " ,线程appyTransition2错误码为；"
                    + appyTransition2.getRetCode() );
        }
    }

    public void test2() throws Exception {
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        lifeCycleConfig = prepareLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );

        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName2 );
        ws.applyTransition( fowlName1 );

        ThreadExecutor t = new ThreadExecutor();
        RemoveTransitionThread removeTransition = new RemoveTransitionThread(
                fowlName1 );
        AppyTransitionThread appyTransition = new AppyTransitionThread(
                fowlName2 );
        t.addWorker( removeTransition );
        t.addWorker( appyTransition );
        t.run();

        if ( removeTransition.getRetCode() == 0 && appyTransition
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
            // 校验removeTransition线程成功场景
            try {
                ws.getTransition( fowlName1 );
                Assert.fail( "预期失败，实际成功！" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.HTTP_NOT_FOUND
                        .getErrorCode() ) {
                    throw e;
                }
            }
        } else if ( removeTransition
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode()
                && appyTransition.getRetCode() == 0 ) {
            // 校验appyTransition线程成功场景
            checkThreadTransition( fowlName2 );
        } else if ( removeTransition.getRetCode() == 0
                && appyTransition.getRetCode() == 0 ) {
            // 校验两个线程都成功场景
            try {
                ws.getTransition( fowlName1 );
                Assert.fail( "预期失败，实际成功！" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.HTTP_NOT_FOUND
                        .getErrorCode() ) {
                    throw e;
                }
            }
            checkThreadTransition( fowlName2 );
        } else {
            Assert.fail( "两个线程预期不该都失败！线程removeTransition错误码为："
                    + removeTransition.getRetCode() + " ,线程appyTransition错误码为；"
                    + appyTransition.getRetCode() );
        }
    }

    public void test3() throws Exception {
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        lifeCycleConfig = prepareLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );

        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName2 );
        ws.applyTransition( fowlName1 );
        ThreadExecutor t = new ThreadExecutor();
        UpdateTransitionThread updateTransitionThread = new UpdateTransitionThread(
                fowlName1 );
        AppyTransitionThread appyTransition = new AppyTransitionThread(
                fowlName2 );
        t.addWorker( updateTransitionThread );
        t.addWorker( appyTransition );
        t.run();

        if ( updateTransitionThread.getRetCode() == 0 && appyTransition
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
            // 校验updateTransitionThread线程成功场景
            ScmLifeCycleTransition actTransition = ws.getTransition( fowlName3 )
                    .getTransition();
            Assert.assertEquals( actTransition.toBSONObject(),
                    updateTransition.toBSONObject() );
        } else if ( updateTransitionThread
                .getRetCode() == ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode()
                && appyTransition.getRetCode() == 0 ) {
            // 校验appyTransition线程成功场景
            checkThreadTransition( fowlName2 );
        } else if ( updateTransitionThread.getRetCode() == 0
                && appyTransition.getRetCode() == 0 ) {
            // 校验两个线程都成功场景
            ScmLifeCycleTransition actTransition = ws.getTransition( fowlName3 )
                    .getTransition();
            Assert.assertEquals( actTransition.toBSONObject(),
                    updateTransition.toBSONObject() );
            checkThreadTransition( fowlName2 );
        } else {
            Assert.fail( "两个线程预期不该都失败！线程updateTransitionThread错误码为："
                    + updateTransitionThread.getRetCode()
                    + " ,线程appyTransition错误码为；" + appyTransition.getRetCode() );
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
        triggers1.add( LifeCycleUtils.initTrigger( "1", "ALL", "1d", "1d", "1d",
                "1d", true ) );
        triggers1.add( LifeCycleUtils.initTrigger( "2", "ANY", "1d", "1d", "1d",
                "1d", true ) );
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

    private class AppyTransitionThread extends ResultStore {
        private String fowlName;

        public AppyTransitionThread( String fowlName ) {
            this.fowlName = fowlName;
        }

        @ExecuteOrder(step = 1)
        private void run() {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                workspace.applyTransition( fowlName );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }

    private class RemoveTransitionThread extends ResultStore {
        private String fowlName;

        public RemoveTransitionThread( String fowlName ) {
            this.fowlName = fowlName;
        }

        @ExecuteOrder(step = 1)
        private void run() {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                workspace.removeTransition( fowlName );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }

    private class UpdateTransitionThread extends ResultStore {
        private String fowlName;

        public UpdateTransitionThread( String fowlName ) {
            this.fowlName = fowlName;
        }

        @ExecuteOrder(step = 1)
        private void run() {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                workspace.updateTransition( fowlName, updateTransition );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }

    public void checkThreadTransition( String fileName ) throws ScmException {
        List< ScmLifeCycleTransition > transitions = lifeCycleConfig
                .getTransitionConfig();

        ScmLifeCycleTransition actTransition = ws.getTransition( fileName )
                .getTransition();

        ScmLifeCycleTransition expTransition = null;

        for ( ScmLifeCycleTransition transition : transitions ) {
            if ( transition.getName() == fileName ) {
                expTransition = transition;
            }
        }
        Assert.assertEquals( actTransition.toBSONObject(),
                expTransition.toBSONObject() );
    }
}