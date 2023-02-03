package com.sequoiacm.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5752:工作区添加Transition验证
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5752 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5752_1";
    private String stageTagName2 = "testTag5752_2";
    private String fowlName1 = "testFowlName5752_1To2";
    private String fowlName2 = "testFowlName5752_2To1";
    private String wsName = "ws5752";
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file5752";
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
        // 指定rootSite创建工作区
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // test a :Transition存在与全局配置中
        test1();

        // test b :Transition不存在于全局配置中
        test2();

        // test c : Transition不存在于工作区
        test3();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                LifeCycleUtils.cleanWsLifeCycleConfig( ws );
                LifeCycleUtils.cleanLifeCycleConfig( session );
                ScmWorkspaceUtil.deleteWs( wsName, session );
                TestTools.LocalFile.removeFile( localPath );
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

        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );

        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName2 );

        createFile();

        ws.applyTransition( fowlName1 );

        SiteWrapper[] expSites = { rootSite };
        ScmScheduleUtils.checkFileMetaAndData( ws, fileIdList, expSites,
                localPath, filePath );
    }

    public void test2() throws Exception {
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        LifeCycleUtils.cleanLifeCycleConfig( session );

        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName2 );
        try {
            ws.applyTransition( fowlName1 + "not_exist" );
            Assert.fail( "预期失败，实际成功" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_INTERNAL_SERVER_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }
    }

    public void test3() throws Exception {
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        LifeCycleUtils.cleanLifeCycleConfig( session );

        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
        try {
            ws.applyTransition( fowlName1 );
            Assert.fail( "预期失败，实际成功" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_NOT_FOUND.getErrorCode() ) {
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

}