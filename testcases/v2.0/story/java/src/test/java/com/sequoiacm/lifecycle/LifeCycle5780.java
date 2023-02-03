package com.sequoiacm.lifecycle;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
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
 * @descreption SCM-5780:工作区添加存在站点阶段标的站点
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5780 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;
    private String stageTagName1 = "testTag5780_1";
    private String stageTagName2 = "testTag5780_2";
    private String fowlName1 = "testFowlName5780_1To2";
    private String fowlName2 = "testFowlName5780_2To1";
    private String fileName = "file5780";
    private ScmWorkspace ws = null;
    private BSONObject queryCond = null;
    private String wsName = "ws5780";
    private String cornRule = "0/1 * * * * ?";

    @BeforeClass
    private void setUp() throws Exception {
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();

        site = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();

        session = TestScmTools.createSession( rootSite );

        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定主站点创建工作区
        ScmWorkspaceUtil.createWS( session, wsName, 1 );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        LifeCycleUtils.cleanLifeCycleConfig( session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        lifeCycleConfig = prepareLifeCycleConfig();

        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
        ScmFactory.Site.setSiteStageTag( session, site.getSiteName(),
                stageTagName1 );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        ScmDataLocation expDataLocation = getDataLocation( site );
        ws.addDataLocation( expDataLocation );

        List< ScmDataLocation > dataLocations = ws.getDataLocations();
        ScmDataLocation actDataLocation = null;
        for ( ScmDataLocation dataLocation1 : dataLocations ) {
            if ( dataLocation1.getSiteName().equals( site.getSiteName() ) ) {
                actDataLocation = dataLocation1;
            }
        }
        Assert.assertEquals( actDataLocation, expDataLocation );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                LifeCycleUtils.cleanWsLifeCycleConfig( ws );
                LifeCycleUtils.cleanLifeCycleConfig( session );
                ScmWorkspaceUtil.deleteWs( wsName, session );
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

    public static ScmDataLocation getDataLocation( SiteWrapper site )
            throws ScmInvalidArgumentException {
        String siteName = site.getSiteName();
        String dataType = site.getDataType().toString();
        ScmDataLocation dataLocation = null;
        switch ( dataType ) {
        case "sequoiadb":
            String domainName = TestSdbTools
                    .getDomainNames( site.getDataDsUrl() ).get( 0 );

            dataLocation = new ScmSdbDataLocation( siteName, domainName );
            break;
        case "hbase":
            dataLocation = new ScmHbaseDataLocation( siteName );
            break;
        case "hdfs":
            dataLocation = new ScmHdfsDataLocation( siteName );
            break;
        case "ceph_s3":
            dataLocation = new ScmCephS3DataLocation( siteName );
            break;
        case "ceph_swift":
            dataLocation = new ScmCephSwiftDataLocation( siteName );
            break;
        case "sftp":
            dataLocation = new ScmSftpDataLocation( siteName );
            break;
        default:
            Assert.fail( "dataSourceType not match: " + dataType );
        }
        return dataLocation;
    }
}