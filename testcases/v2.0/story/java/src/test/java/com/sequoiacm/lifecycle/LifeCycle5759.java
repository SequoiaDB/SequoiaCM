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
 * @descreption SCM-5759:不同用户更新工作区下Transition
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5759 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5759_1";
    private String stageTagName2 = "testTag5759_2";
    private String fowlName1 = "testFowlName5759_1To2";
    private String fowlName2 = "testFowlName5759_2To1";
    private String fileName = "file5759";
    private WsWrapper wsp = null;

    private ScmWorkspace ws = null;
    private BSONObject queryCond = null;
    private String cornRule = "0/1 * * * * ?";
    private String user = "user5759";

    @BeforeClass
    private void setUp() throws Exception {
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();

        site = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );

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
        ScmSession ss = ScmSessionUtils.createSession( site, user, user );
        ScmWorkspace workspace = ScmFactory.Workspace
                .getWorkspace( wsp.getName(), ss );

        ScmLifeCycleTransition scmLifeCycleTransition = prepareTransitionConfig(
                fowlName2, stageTagName2, stageTagName1 );

        try {
            workspace.updateTransition( fowlName1, scmLifeCycleTransition );
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

        // 验证实际未被修改
        ScmLifeCycleTransition actTransition = ws.getTransition( fowlName1 )
                .getTransition();
        ScmLifeCycleTransition expTransition = null;
        List< ScmLifeCycleTransition > transitionConfig = lifeCycleConfig
                .getTransitionConfig();
        for ( ScmLifeCycleTransition transition : transitionConfig ) {
            if ( transition.getName() == fowlName1 ) {
                expTransition = transition;
            }
        }
        Assert.assertEquals( actTransition.toBSONObject(),
                expTransition.toBSONObject() );

    }

    public void test2() throws ScmException {
        ScmLifeCycleTransition scmLifeCycleTransition = prepareTransitionConfig(
                fowlName2, stageTagName2, stageTagName1 );
        ws.updateTransition( fowlName1, scmLifeCycleTransition );

        ScmTransitionSchedule transition = ws.getTransition( fowlName2 );
        // 验证工作区下Transition是否修改
        List< ScmLifeCycleTransition > expScmLifeCycleTransitionList = new ArrayList<>();
        expScmLifeCycleTransitionList.add( scmLifeCycleTransition );
        List< ScmLifeCycleTransition > actScmLifeCycleTransitionList = new ArrayList<>();
        actScmLifeCycleTransitionList.add( transition.getTransition() );
        LifeCycleUtils.checkTransitionConfig( actScmLifeCycleTransitionList,
                expScmLifeCycleTransitionList );
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.deleteRole( session, user );
                ScmAuthUtils.deleteUser( session, user );
            }
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

    public ScmLifeCycleTransition prepareTransitionConfig( String fowlName,
            String sourceStageTag, String DestStageTag ) {
        // 文件流转触发器配置
        List< ScmTrigger > triggers1 = new ArrayList<>();
        triggers1.add( LifeCycleUtils.initTrigger( "1", "ANY", "0d", "1d", "1d",
                "1d", true ) );
        ScmTransitionTriggers transitionTriggers = LifeCycleUtils
                .initScmTransitionTriggers( cornRule, "ANY", 300000,
                        triggers1 );

        // Transition配置
        ScmLifeCycleTransition scmLifeCycleTransition1 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName, sourceStageTag,
                        DestStageTag, transitionTriggers, null, queryCond,
                        "strict", false, true, "CURRENT" );

        return scmLifeCycleTransition1;
    }
}