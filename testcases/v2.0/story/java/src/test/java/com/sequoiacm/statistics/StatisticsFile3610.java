package com.sequoiacm.statistics;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @Description: SCM-3610:scm.statistics.timeGranularity配置功能验证
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3610 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3610";
    private List< ScmId > fileIdList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理文件
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // 更新gateway配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
    }

    @Test
    private void test() throws Exception {
        // 更新admin-server配置timeGranularity为DAY
        Map< String, String > confMap = new HashMap<>();
        confMap.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap );

        // 上传文件
        fileIdList.add( createFile( ws ) );
        StatisticsUtils.waitStatisticalInfoCount( 1 );
        ScmFileStatisticInfo statisticInfo1 = queryStatisticInfo(
                ScmTimeAccuracy.DAY );
        // 检查结果
        Assert.assertEquals( statisticInfo1.getRequestCount(), 1 );
        Assert.assertEquals( statisticInfo1.getAvgTrafficSize(), 0 );

        // 更新admin-server配置timeGranularity为HOUR
        confMap.put( "scm.statistics.timeGranularity", "HOUR" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap );
        // 上传文件
        fileIdList.add( createFile( ws ) );
        StatisticsUtils.waitStatisticalInfoCount( 2 );
        ScmFileStatisticInfo statisticInfo2 = queryStatisticInfo(
                ScmTimeAccuracy.HOUR );
        Assert.assertTrue(
                statisticInfo2.getRequestCount() == 2
                        || statisticInfo2.getRequestCount() == 1,
                "" + statisticInfo2.getRequestCount() );
        Assert.assertEquals( statisticInfo2.getAvgTrafficSize(), 0 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                StatisticsUtils.clearStatisticalInfo();
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } finally {
            ConfUtil.deleteGateWayStatisticalConf();
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmFileStatisticInfo queryStatisticInfo( ScmTimeAccuracy scmTimeAccuracy ) throws ScmException {
        // 查询上传接口统计信息
        Date now = new Date();
        ScmFileStatisticInfo statisticInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( new Date( now.getTime() - 1000 * 60 * 60 * 5 ) )
                .endDate( new Date( now.getTime() + 1000 * 60 * 60 * 24 * 3 ) )
                .timeAccuracy( scmTimeAccuracy ).upload().get();
        return statisticInfo;
    }

    private ScmId createFile( ScmWorkspace ws ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "_" + UUID.randomUUID() );
        file.setAuthor( fileName );
        return file.save();
    }
}