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
 * @descreption SCM-5772:添加全局阶段标签与删除全局阶段标签并发
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5772 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5772_1";
    private String stageTagName2 = "testTag5772_2";
    private String stageTagName3 = "testTag5772_3";
    private String stageTagName4 = "testTag5772_4";
    private String fowlName1 = "testFowlName5772_1To2";
    private String fowlName2 = "testFowlName5772_2To1";
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

        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        lifeCycleConfig = prepareLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        DeleteSiteStageTagThread deleteSiteStageTag = new DeleteSiteStageTagThread();
        AddSiteStageTagThread addSiteStageTag = new AddSiteStageTagThread();
        t.addWorker( deleteSiteStageTag );
        t.addWorker( addSiteStageTag );
        t.run();

        ScmLifeCycleConfig actLifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        if ( deleteSiteStageTag.getRetCode() == 0
                && addSiteStageTag.getRetCode() == 0 ) {
            // 校验两个线程都成功场景
            List< ScmLifeCycleStageTag > stageTags = actLifeCycleConfig
                    .getStageTagConfig();
            List< ScmLifeCycleStageTag > expStageTags = new ArrayList<>();
            expStageTags.add(
                    new ScmLifeCycleStageTag( stageTagName1, stageTagName1 ) );
            expStageTags.add(
                    new ScmLifeCycleStageTag( stageTagName2, stageTagName2 ) );
            expStageTags.add(
                    new ScmLifeCycleStageTag( stageTagName4, stageTagName4 ) );
            LifeCycleUtils.checkStageTagConfig( stageTags, expStageTags );
        } else {
            Assert.fail( "两个线程预期不该都失败！线程addSiteStageTag错误码为："
                    + addSiteStageTag.getRetCode()
                    + " ,线程deleteSiteStageTag错误码为；"
                    + deleteSiteStageTag.getRetCode() );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            LifeCycleUtils.cleanWsLifeCycleConfig( ws );
            LifeCycleUtils.cleanLifeCycleConfig( session );
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
        ScmLifeCycleStageTag testStageTag3 = new ScmLifeCycleStageTag();
        testStageTag3.setName( stageTagName3 );
        testStageTag3.setDesc( stageTagName3 );

        stageTagConfig.add( testStageTag1 );
        stageTagConfig.add( testStageTag2 );
        stageTagConfig.add( testStageTag3 );

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

    private class AddSiteStageTagThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = TestScmTools.createSession( site )) {
                ScmSystem.LifeCycleConfig.addStageTag( session, stageTagName4,
                        stageTagName4 );
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
                        stageTagName3 );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }
}