package com.sequoiacm.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @author ZhangYanan
 * @version 1.0
 * @descreption SCM-5922:工作区配置Transition后，修改站点阶段标签
 * @date 2023/2/08
 * @updateUser
 * @updateDate
 * @updateRemark
 */
public class LifeCycle5922 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site1 = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5922_1";
    private String stageTagName2 = "testTag5922_2";
    private String stageTagName3 = "testTag5922_3";
    private String fowlName1 = "testFowlName5922_1To2";
    private String fileName = "file5922";
    private int fileSize = 1024 * 1024;
    private int fileNum = 5;
    private File localPath = null;
    private String filePath = null;
    private String wsName = "ws5922";
    private ScmWorkspace ws = null;
    private BSONObject queryCond = null;
    private String cornRule = "0/1 * * * * ?";
    private List< ScmId > fileIdList = new ArrayList<>();
    private List< String > fileIdsToString = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site1 = ScmInfo.getBranchSite();

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );

        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 主站点准备文件
        createFile( ws );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .in( fileIdsToString ).get();

        // 设置全局生命周期规则
        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        lifeCycleConfig = prepareLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName1 );
        ScmFactory.Site.setSiteStageTag( session, site1.getSiteName(),
                stageTagName2 );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // 设置fowlName1，文件流向:主站点->site1
        ws.applyTransition( fowlName1 );

        // 验证配置的Transition是否生效
        SiteWrapper[] expSites1 = { site1 };
        ScmScheduleUtils.checkFileMetaAndData( ws, fileIdList, expSites1,
                localPath, filePath );

        ScmFactory.Site.alterSiteStageTag( session, site1.getSiteName(),
                stageTagName3 );

        // 验证配置的fowlName1
        checkTransition();

        ws.removeTransition( fowlName1 );
        ScmFactory.Site.alterSiteStageTag( session, site1.getSiteName(),
                stageTagName2 );

        // 迁移文件回主站点
        testMoveFileTask();

        // 设置fowlName1，文件流向:主站点->site1
        ws.applyTransition( fowlName1 );

        // 验证配置的Transition是否生效
        ScmScheduleUtils.checkFileMetaAndData( ws, fileIdList, expSites1,
                localPath, filePath );
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

        // 迁移所有版本文件
        List< ScmTrigger > triggers1 = new ArrayList<>();
        triggers1.add( LifeCycleUtils.initTrigger( "1", "ALL", "0d", "0d", "0d",
                "0d", true ) );
        triggers1.add( LifeCycleUtils.initTrigger( "2", "ANY", "0d", "0d", "0d",
                "0d", true ) );
        ScmTransitionTriggers transitionTriggers1 = LifeCycleUtils
                .initScmTransitionTriggers( cornRule, "ANY", 300000,
                        triggers1 );

        ScmLifeCycleTransition scmLifeCycleTransition1 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName1, stageTagName1,
                        stageTagName2, transitionTriggers1, null, queryCond,
                        "strict", false, true, "ALL" );

        List< ScmLifeCycleTransition > transitionConfig = new ArrayList<>();
        transitionConfig.add( scmLifeCycleTransition1 );

        // 组装为lifeCycleConfig
        ScmLifeCycleConfig lifeCycleConfig = new ScmLifeCycleConfig();
        lifeCycleConfig.setStageTagConfig( stageTagConfig );
        lifeCycleConfig.setTransitionConfig( transitionConfig );
        return lifeCycleConfig;
    }

    public void createFile( ScmWorkspace ws )
            throws ScmException, InterruptedException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileName );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
            fileIdsToString.add( fileId.toString() );
        }
    }

    public void checkTransition() throws ScmException {
        // 校验工作区中Transition同步改变
        ScmLifeCycleTransition expWsTransition1 = null;
        List< ScmLifeCycleTransition > transitionConfig = lifeCycleConfig
                .getTransitionConfig();
        for ( ScmLifeCycleTransition transition : transitionConfig ) {
            if ( transition.getName().equals( fowlName1 ) ) {
                expWsTransition1 = transition;
            }
        }
        assert expWsTransition1 != null;
        expWsTransition1.setDest( stageTagName3 );

        ScmLifeCycleTransition actTransition1 = ws.getTransition( fowlName1 )
                .getTransition();
        Assert.assertEquals( actTransition1.toBSONObject(),
                expWsTransition1.toBSONObject() );

        // 验证全局配置中Transition1未改变
        ScmLifeCycleConfig actLifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        lifeCycleConfig = prepareLifeCycleConfig();
        LifeCycleUtils.checkScmLifeCycleConfig( actLifeCycleConfig,
                lifeCycleConfig );
    }

    private void testMoveFileTask() throws Exception {
        try ( ScmSession session = ScmSessionUtils.createSession( site1 )) {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );

            ScmMoveTaskConfig config = new ScmMoveTaskConfig();
            config.setWorkspace( ws );
            config.setTargetSite( rootSite.getSiteName() );
            config.setScope( ScmType.ScopeType.SCOPE_CURRENT );
            config.setDataCheckLevel( ScmDataCheckLevel.STRICT );
            config.setQuickStart( true );
            config.setCondition( queryCond );
            config.setRecycleSpace( true );
            ScmId task = ScmSystem.Task.startMoveTask( config );
            ScmTaskUtils.waitTaskFinish( session, task );
            SiteWrapper[] expSites1 = { rootSite };
            ScmScheduleUtils.checkFileMetaAndData( ws, fileIdList, expSites1,
                    localPath, filePath );
        }
    }
}