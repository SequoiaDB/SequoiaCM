package com.sequoiacm.statistics;

import java.io.ByteArrayOutputStream;
import java.util.*;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @Description: SCM-3605:.xx-type.conditions.workspaces和xx-type.connditions.workspaceRegex配置项测试
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3605 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp1 = null;
    private WsWrapper wsp2 = null;
    private ScmSession session = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;
    private String fileName = "file3605";
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;

    @BeforeClass
    private void setUp() throws Exception {
        if ( ScmInfo.getWsNum() < 2 ) {
            throw new SkipTestException( "跳过此用例!!!" );
        }
        site = ScmInfo.getSite();
        List< WsWrapper > wsList = ScmInfo.getAllWorkspaces();
        wsp1 = wsList.get( 0 );
        wsp2 = wsList.get( 1 );
        session = ScmSessionUtils.createSession( site );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp1.getName(), session );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp2.getName(), session );
        // 清理文件
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp1, cond );
        ScmFileUtils.cleanFile( wsp2, cond );
        fileId1 = createFile( ws1 );
        fileId2 = createFile( ws2 );

        ConfUtil.deleteGateWayStatisticalConf();
        // 更新admin-server配置
        Map< String, String > confMap = new HashMap<>();
        confMap.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap );
    }

    @Test
    private void test() throws Exception {
        // 无包含关系
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download" );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaces",
                wsp1.getName() );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaceRegex",
                wsp2.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1000" );
        ScmUpdateConfResultSet result1 = ConfUtil
                .updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        Assert.assertEquals( result1.getSuccesses().size() > 0, true );
        Assert.assertEquals( result1.getFailures().size(), 0 );
        // 下载文件
        downloadFile( ws1, fileId1 );
        downloadFile( ws2, fileId2 );
        StatisticsUtils.waitStatisticalInfoCount( 2 );
        queryStatisticInfoAndCheck( 2 );

        ConfUtil.deleteGateWayStatisticalConf();
        StatisticsUtils.clearStatisticalInfo();

        // 有包含关系
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaces",
                wsp1.getName() );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaceRegex",
                wsp2.getName() + "|" + wsp1.getName() );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        downloadFile( ws1, fileId1 );
        downloadFile( ws2, fileId2 );
        StatisticsUtils.waitStatisticalInfoCount( 2 );
        queryStatisticInfoAndCheck( 2 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws1, fileId1, true );
                ScmFactory.File.deleteInstance( ws2, fileId2, true );
            }
        } finally {
            ConfUtil.deleteGateWayStatisticalConf();
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void queryStatisticInfoAndCheck( long requestCount )
            throws ScmException {
        // 查询上传接口统计信息
        Date now = new Date();
        ScmFileStatisticInfo statisticInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( new Date( now.getTime() - 1000 * 60 * 60 * 24 ) )
                .endDate( new Date( now.getTime() + 1000 * 60 * 60 * 24 * 3 ) )
                .timeAccuracy( ScmTimeAccuracy.DAY ).download().get();
        // 检查结果
        Assert.assertEquals( statisticInfo.getRequestCount(), requestCount );
        Assert.assertEquals( statisticInfo.getAvgTrafficSize(), 0 );
    }

    private void downloadFile( ScmWorkspace ws, ScmId fileId )
            throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.getContent( new ByteArrayOutputStream() );
    }

    private ScmId createFile( ScmWorkspace ws ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "_" + UUID.randomUUID() );
        file.setAuthor( fileName );
        return file.save();
    }
}