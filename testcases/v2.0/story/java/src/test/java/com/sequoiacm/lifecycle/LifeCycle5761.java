package com.sequoiacm.lifecycle;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @descreption SCM-5761:不同用户查询工作区下Transition
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5761 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5761_1";
    private String stageTagName2 = "testTag5761_2";
    private String fowlName1 = "testFowlName5761_1To2";
    private String fowlName2 = "testFowlName5761_2To1";
    private String fileName = "file5761";
    private WsWrapper wsp = null;

    private ScmWorkspace ws = null;
    private BSONObject queryCond = null;
    private String cornRule = "0/1 * * * * ?";
    private String user = "user5761";

    @BeforeClass
    private void setUp() throws Exception {
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();

        site = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );

        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        ScmAuthUtils.deleteUser( session, user );
        createUser();

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
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // test a : 无工作区权限用户
        test1();

        // test b : 有工作区权限用户
        test2();
        runSuccess = true;
    }

    public void test1() throws ScmException, InterruptedException {
        ScmSession ss = TestScmTools.createSession( site, user, user );
        ScmWorkspace workspace = ScmFactory.Workspace
                .getWorkspace( wsp.getName(), ss );

        try {
            workspace.getTransition( fowlName1 );
            Assert.fail( "预期失败实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) {
                throw e;
            }
        } finally {
            if ( ss != null ) {
                ss.close();
            }
        }
    }

    public void test2() throws ScmException {
        ScmTransitionSchedule transition = ws.getTransition( fowlName1 );
        ScmLifeCycleTransition expTransition = null;
        List< ScmLifeCycleTransition > transitionConfig = lifeCycleConfig
                .getTransitionConfig();
        for ( ScmLifeCycleTransition scmLifeCycleTransition : transitionConfig ) {
            if ( scmLifeCycleTransition.getName() == fowlName1 ) {
                expTransition = scmLifeCycleTransition;
            }
        }

        List< ScmLifeCycleTransition > expScmLifeCycleTransitionList = new ArrayList<>();
        expScmLifeCycleTransitionList.add( expTransition );
        List< ScmLifeCycleTransition > actScmLifeCycleTransitionList = new ArrayList<>();
        actScmLifeCycleTransitionList.add( transition.getTransition() );
        LifeCycleUtils.checkTransitionConfig( actScmLifeCycleTransitionList,
                expScmLifeCycleTransitionList );
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                LifeCycleUtils.cleanWsLifeCycleConfig( ws );
                LifeCycleUtils.cleanLifeCycleConfig( session );
                ScmFactory.Role.deleteRole( session, user );
                ScmAuthUtils.deleteUser( session, user );
            }
        } finally {
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

    private void createUser() throws ScmException {
        // create user
        ScmUser scmUser = ScmFactory.User.createUser( session, user,
                ScmUserPasswordType.LOCAL, user );
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.createRole( session, user, "" );
        modifier.addRole( user );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }
}