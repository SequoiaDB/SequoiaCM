package com.sequoiacm.statistics;

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
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @Description: SCM-3604:scm.statistics.types.xx-type.conditions.workspaceRegex配置项测试
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3604 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp1 = null;
    private WsWrapper wsp2 = null;
    private ScmSession session = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;
    private String fileName = "file3604";
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        if ( ScmInfo.getWsNum() < 2 ) {
            throw new SkipTestException( "跳过此用例!!!" );
        }
        site = ScmInfo.getSite();
        List< WsWrapper > wsList = ScmInfo.getAllWorkspaces();
        wsp1 = wsList.get( 0 );
        wsp2 = wsList.get( 1 );
        session = TestScmTools.createSession( site );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp1.getName(), session );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp2.getName(), session );
        ConfUtil.deleteGateWayStatisticalConf();
        // 清理文件
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp1, cond );
        ScmFileUtils.cleanFile( wsp2, cond );
        // 更新admin-server配置
        Map< String, String > confMap = new HashMap<>();
        confMap.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap );
    }

    @Test
    private void test() throws Exception {
        // 匹配不到工作区
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put(
                "scm.statistics.types.file_upload.conditions.workspaceRegex",
                "^3604.*$" );
        confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1" );
        ScmUpdateConfResultSet result1 = ConfUtil
                .updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        Assert.assertEquals( result1.getSuccesses().size() > 0, true );
        Assert.assertEquals( result1.getFailures().size(), 0 );

        // 上传文件
        fileIdList1.add( createFile( ws1 ) );
        queryStatisticInfoAndCheck( 0 );

        ConfUtil.deleteGateWayStatisticalConf();

        // 匹配到单个工作区
        confMap1.put(
                "scm.statistics.types.file_upload.conditions.workspaceRegex",
                wsp1.getName() );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        // 上传文件
        fileIdList1.add( createFile( ws1 ) );
        StatisticsUtils.waitStatisticalInfoCount( 1 );
        queryStatisticInfoAndCheck( 1 );

        // 匹配到多个工作区
        confMap1.put(
                "scm.statistics.types.file_upload.conditions.workspaceRegex",
                ".*" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        fileIdList2.add( createFile( ws2 ) );
        StatisticsUtils.waitStatisticalInfoCount( 2 );
        queryStatisticInfoAndCheck( 2 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws1, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws2, fileId, true );
                }
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
                .timeAccuracy( ScmTimeAccuracy.DAY ).upload().get();
        // 检查结果
        Assert.assertEquals( statisticInfo.getRequestCount(), requestCount );
        Assert.assertEquals( statisticInfo.getAvgTrafficSize(), 0 );
    }

    private ScmId createFile( ScmWorkspace ws ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "_" + UUID.randomUUID() );
        file.setAuthor( fileName );
        return file.save();
    }
}