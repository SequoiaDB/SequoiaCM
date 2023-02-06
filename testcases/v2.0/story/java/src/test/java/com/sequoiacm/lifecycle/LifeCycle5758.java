package com.sequoiacm.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5758:工作区更新Transition验证
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5758 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5758_1";
    private String stageTagName2 = "testTag5758_2";
    private String fowlName1 = "testFowlName5758_1To2";
    private String fowlName2 = "testFowlName5758_2To1";
    private String wsName = "ws5758";
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file5758";
    private int fileSize = 1024 * 1024;
    private int fileNum = 20;
    private ArrayList< ScmId > fileIdList = new ArrayList<>();
    private ScmWorkspace ws = null;
    private BSONObject queryCond = null;
    private String cornRule = "0/1 * * * * ?";

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();

        site = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();

        session = TestScmTools.createSession( site );
        lifeCycleConfig = prepareLifeCycleConfig();

        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建文件至site
        createFile();

        // 工作区配置
        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );

        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );

        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName2 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // test a :Transition存在于全局配置中
        test1();
        // test b :Transition不存在于全局配置中
        test2();
        // test c :Transition中使用的阶段标签不存在于全局配置
        test3();
        // test d :Transition中使用的阶段标签不存在于工作区
        test4();
        // test e :Transition已存在于工作区
        test5();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            LifeCycleUtils.cleanWsLifeCycleConfig( ws );
            LifeCycleUtils.cleanLifeCycleConfig( session );
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void test1() throws Exception {
        ws.applyTransition( fowlName1 );
        // 验证配置的Transition是否生效
        SiteWrapper[] expSites1 = { rootSite };
        ScmScheduleUtils.checkScmFile( ws, fileIdList, expSites1 );

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

        // 验证修改后的Transition是否生效
        SiteWrapper[] expSites2 = { site };
        ScmScheduleUtils.checkScmFile( ws, fileIdList, expSites2 );

        ws.removeTransition( fowlName2 );
    }

    public void test2() throws Exception {
        ws.applyTransition( fowlName1 );
        // 验证配置的Transition是否生效
        SiteWrapper[] expSites1 = { rootSite };
        ScmScheduleUtils.checkScmFile( ws, fileIdList, expSites1 );

        ScmLifeCycleTransition scmLifeCycleTransition = prepareTransitionConfig(
                fowlName2 + "not_exit", stageTagName2, stageTagName1 );
        ws.updateTransition( fowlName1, scmLifeCycleTransition );

        ScmTransitionSchedule transition = ws
                .getTransition( fowlName2 + "not_exit" );

        // 验证工作区下Transition是否修改
        List< ScmLifeCycleTransition > expScmLifeCycleTransitionList = new ArrayList<>();
        expScmLifeCycleTransitionList.add( scmLifeCycleTransition );
        List< ScmLifeCycleTransition > actScmLifeCycleTransitionList = new ArrayList<>();
        actScmLifeCycleTransitionList.add( transition.getTransition() );
        LifeCycleUtils.checkTransitionConfig( actScmLifeCycleTransitionList,
                expScmLifeCycleTransitionList );

        // 验证修改后的Transition是否生效
        SiteWrapper[] expSites2 = { site };
        ScmScheduleUtils.checkScmFile( ws, fileIdList, expSites2 );

        ws.removeTransition( fowlName2 + "not_exit" );
    }

    public void test3() throws Exception {
        ws.applyTransition( fowlName1 );
        // 验证配置的Transition是否生效
        SiteWrapper[] expSites1 = { rootSite };
        ScmScheduleUtils.checkScmFile( ws, fileIdList, expSites1 );

        ScmLifeCycleTransition scmLifeCycleTransition = prepareTransitionConfig(
                fowlName2, stageTagName2 + "not_exit", stageTagName1 );
        try {
            ws.updateTransition( fowlName1, scmLifeCycleTransition );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }

        }
    }

    public void test4() throws Exception {
        // 使用内置标签构造存在于全局配置不存在于工作区下站点场景
        ScmLifeCycleTransition scmLifeCycleTransition = prepareTransitionConfig(
                fowlName2, "Hot", stageTagName1 );
        try {
            ws.updateTransition( fowlName1, scmLifeCycleTransition );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_NOT_FOUND.getErrorCode() ) {
                throw e;
            }

        }
    }

    public void test5() throws Exception {
        ws.applyTransition( fowlName2 );

        ScmLifeCycleTransition scmLifeCycleTransition = prepareTransitionConfig(
                fowlName2, stageTagName2, stageTagName1 );
        try {
            ws.updateTransition( fowlName1, scmLifeCycleTransition );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_INTERNAL_SERVER_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }
    }

    public void createFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( fileName + i );
            file.setAuthor( fileName );
            ScmId scmId = file.save();
            fileIdList.add( scmId );
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