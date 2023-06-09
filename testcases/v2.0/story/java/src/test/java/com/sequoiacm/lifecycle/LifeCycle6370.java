package com.sequoiacm.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmOnceTransitionConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.element.lifecycle.ScmTransitionTriggers;
import com.sequoiacm.client.element.lifecycle.ScmTrigger;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @descreption SCM-6370:一次性迁移startOnceTransitionV2接口功能验证
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle6370 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag6370_1";
    private String stageTagName2 = "testTag6370_2";
    private String fowlName1 = "testFowlName6370_1To2";
    private String fowlName2 = "testFowlName6370_2To1";
    private File localPath = null;
    private String filePath = null;

    private WsWrapper wsp = null;
    private String fileName = "file6370";
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

        wsp = ScmInfo.getWs();
        site = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();

        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        // 创建文件至site
        createFile();

        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        lifeCycleConfig = prepareLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );

        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                stageTagName2 );

    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        // 验证迁移清理任务
        ScmOnceTransitionConfig scmOnceTransitionConfig = new ScmOnceTransitionConfig(
                "move_file", ws, 300000, queryCond, stageTagName1,
                stageTagName2, TestScmBase.defaultRegion, TestScmBase.zone1 );
        scmOnceTransitionConfig.setRecycleSpace( false );
        scmOnceTransitionConfig.setScope( ScmType.ScopeType.SCOPE_CURRENT );

        ScmSystem.LifeCycleConfig
                .startOnceTransitionV2( scmOnceTransitionConfig );

        SiteWrapper[] expSites = { rootSite };
        ScmScheduleUtils.checkScmFile( ws, fileIdList, expSites );

        // 验证迁移任务
        scmOnceTransitionConfig = new ScmOnceTransitionConfig( "copy_file", ws,
                300000, queryCond, stageTagName2, stageTagName1,
                TestScmBase.defaultRegion, TestScmBase.zone1 );
        scmOnceTransitionConfig.setRecycleSpace( false );
        scmOnceTransitionConfig.setScope( ScmType.ScopeType.SCOPE_CURRENT );

        ScmSystem.LifeCycleConfig
                .startOnceTransitionV2( scmOnceTransitionConfig );

        SiteWrapper[] expSites2 = { rootSite, site };
        ScmScheduleUtils.checkScmFile( ws, fileIdList, expSites2 );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            LifeCycleUtils.cleanWsLifeCycleConfig( ws );
            LifeCycleUtils.cleanLifeCycleConfig( session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void createFile() throws ScmException {
        ScmFileUtils.cleanFile( wsp, queryCond );
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