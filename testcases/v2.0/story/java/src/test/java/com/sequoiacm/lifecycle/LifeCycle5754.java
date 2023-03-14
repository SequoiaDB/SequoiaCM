package com.sequoiacm.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5754:工作区添加Transition与流属性字段验证
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5754 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site1 = null;
    private SiteWrapper site2 = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5754_1";
    private String stageTagName2 = "testTag5754_2";
    private String stageTagName3 = "testTag5754_3";
    private String fowlName1 = "testFowlName5754_1To2";
    private String fowlName2 = "testFowlName5754_2To1";
    private String fowlName3 = "testFowlName5754_1To3";
    private String fowlName4 = "testFowlName5754_3To1";
    private String fileName = "file5754";
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private String fileUpdatePath = null;
    private String wsName = "ws5754";
    private ScmWorkspace ws = null;
    private BSONObject queryCond = null;
    private String cornRule = "0/1 * * * * ?";
    private int fileNum = 1;

    private List< ScmId > fileIdList = new ArrayList<>();
    private List< String > fileIdsToString = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        fileUpdatePath = localPath + File.separator + "localUpdateFile_"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( fileUpdatePath, fileSize );

        List< SiteWrapper > branchSites = ScmInfo.getBranchSites();
        site1 = branchSites.get( 0 );
        site2 = branchSites.get( 1 );
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
        ScmFactory.Site.setSiteStageTag( session, site2.getSiteName(),
                stageTagName3 );

    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // 设置fowlName1，文件流向:主站点->site1
        ws.applyTransition( fowlName1 );

        // 验证配置的Transition是否生效
        SiteWrapper[] expSites1 = { site1 };
        ScmScheduleUtils.checkFileMetaAndData( ws, fileIdList, expSites1,
                localPath, fileUpdatePath );
        ScmScheduleUtils.checkHistoryFileMetaAndData( ws, fileIdList, expSites1,
                localPath, filePath, 1, 0 );
        ws.removeTransition( fowlName1 );

        // 设置fowlName2，文件流向:site->主站点
        ws.applyTransition( fowlName2 );

        // 验证配置的Transition是否生效
        SiteWrapper[] expSites2 = { rootSite };
        ScmScheduleUtils.checkFileMetaAndData( ws, fileIdList, expSites2,
                localPath, fileUpdatePath );
        ScmScheduleUtils.checkHistoryFileMetaAndData( ws, fileIdList, expSites2,
                localPath, filePath, 1, 0 );
        ws.removeTransition( fowlName2 );

        // 设置fowlName3，文件流向:主站点->site2
        ws.applyTransition( fowlName3 );
        SiteWrapper[] expSites3 = { site2 };
        ScmScheduleUtils.checkFileMetaAndData( ws, fileIdList, expSites3,
                localPath, fileUpdatePath );
        ScmScheduleUtils.checkHistoryFileMetaAndData( ws, fileIdList, expSites3,
                localPath, filePath, 1, 0 );
        ws.removeTransition( fowlName3 );

        // 设置fowlName4，文件历史版本流向:site2->主站点
        ws.applyTransition( fowlName4 );
        ScmScheduleUtils.checkFileMetaAndData( ws, fileIdList, expSites3,
                localPath, fileUpdatePath );
        ScmScheduleUtils.checkHistoryFileMetaAndData( ws, fileIdList, expSites2,
                localPath, filePath, 1, 0 );
        ws.removeTransition( fowlName4 );

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

        // 迁移所有版本文件，触发器规则验证不同mode和createTime
        List< ScmTrigger > triggers1 = new ArrayList<>();
        triggers1.add( LifeCycleUtils.initTrigger( "1", "ALL", "0d", "0d", "0d",
                "0d", true ) );
        triggers1.add( LifeCycleUtils.initTrigger( "2", "ANY", "0d", "0d", "0d",
                "0d", true ) );
        ScmTransitionTriggers transitionTriggers1 = LifeCycleUtils
                .initScmTransitionTriggers( cornRule, "ANY", 300000,
                        triggers1 );

        List< ScmTrigger > triggers2 = new ArrayList<>();
        triggers2.add( LifeCycleUtils.initTrigger( "1", "ANY", "0d", "0d", "0d",
                "0d", true ) );
        ScmTransitionTriggers transitionTriggers2 = LifeCycleUtils
                .initScmTransitionTriggers( cornRule, "ANY", 300000,
                        triggers2 );

        ScmLifeCycleTransition scmLifeCycleTransition1 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName1, stageTagName1,
                        stageTagName2, transitionTriggers1, null, queryCond,
                        "strict", false, true, "ALL" );

        ScmLifeCycleTransition scmLifeCycleTransition2 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName2, stageTagName2,
                        stageTagName1, transitionTriggers2, null, queryCond,
                        "strict", false, true, "ALL" );

        // 迁移所有版本文件，触发器规则验证buildTime
        List< ScmTrigger > triggers3 = new ArrayList<>();
        triggers3.add( LifeCycleUtils.initTrigger( "1", "ANY", "0d", "30d",
                "30d", "30d", true ) );
        ScmTransitionTriggers transitionTriggers3 = LifeCycleUtils
                .initScmTransitionTriggers( cornRule, "ANY", 300000,
                        triggers3 );

        ScmLifeCycleTransition scmLifeCycleTransition3 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName3, stageTagName1,
                        stageTagName3, transitionTriggers3, null, queryCond,
                        "strict", false, true, "ALL" );

        // 迁移历史文件，触发器规则验证lastAccessTime
        List< ScmTrigger > triggers4 = new ArrayList<>();
        triggers4.add( LifeCycleUtils.initTrigger( "1", "ANY", "30d", "30d",
                "30d", "0d", true ) );
        ScmTransitionTriggers transitionTriggers4 = LifeCycleUtils
                .initScmTransitionTriggers( cornRule, "ANY", 300000,
                        triggers4 );

        ScmLifeCycleTransition scmLifeCycleTransition4 = LifeCycleUtils
                .initScmLifeCycleTransition( fowlName4, stageTagName3,
                        stageTagName1, transitionTriggers4, null, queryCond,
                        "strict", false, true, "HISTORY" );

        List< ScmLifeCycleTransition > transitionConfig = new ArrayList<>();
        transitionConfig.add( scmLifeCycleTransition1 );
        transitionConfig.add( scmLifeCycleTransition2 );
        transitionConfig.add( scmLifeCycleTransition3 );
        transitionConfig.add( scmLifeCycleTransition4 );

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
            ScmId fileID = file.save();
            fileIdList.add( fileID );
            fileIdsToString.add( fileID.toString() );
            file.updateContent( fileUpdatePath );
        }
    }
}