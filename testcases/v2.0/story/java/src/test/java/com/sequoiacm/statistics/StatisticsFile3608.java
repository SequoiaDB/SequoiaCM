package com.sequoiacm.statistics;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3608:scm.statistics.
 *               rawDataCacheSize和scm.statistics.rawDataReportPeriod配置项功能验证
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3608 extends TestScmBase {
    private AtomicInteger actSuccessCount = new AtomicInteger( 0 );
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3608";
    private String gateWayUrl = null;
    private List< ScmId > fileIdList = new CopyOnWriteArrayList<>();
    private byte[] bytes = new byte[ 1 ];

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        gateWayUrl = TestScmBase.gateWayList.get( 0 );
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        new Random().nextBytes( bytes );
        // 清理文件
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // 更新admin-server配置
        Map< String, String > confMap = new HashMap<>();
        confMap.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap );
    }

    @DataProvider(name = "dataProvider")
    public Object[][] generateData() {
        return new Object[][] {
                // 请求次数等于rawDataCacheSize的1/5且在
                // scm.statistics.rawDataReportPeriod周期内
                { 4, 20, 60 * 60 * 1000 },
                // 请求次数小于rawDataCacheSize的1/5，scm.statistics.rawDataReportPeriod周期后
                { 3, 20, 1 * 1000 },
                // 请求次数大于rawCacheSize的1/5且在scm.statistics.rawDataReportPeriod周期后
                { 5, 20, 1 * 1000 } };
    }

    @Test(dataProvider = "dataProvider")
    private void test( int threadNum, int rawDataCacheSize,
            int rawDataReportPeriod ) throws Exception {
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize",
                String.valueOf( rawDataCacheSize ) );
        confMap1.put( "scm.statistics.rawDataReportPeriod",
                String.valueOf( rawDataReportPeriod ) );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        // 并发上传文件
        ThreadExecutor threadExecutor = new ThreadExecutor();
        for ( int i = 0; i < threadNum; i++ ) {
            threadExecutor.addWorker( new CreateFile() );
        }
        threadExecutor.run();
        StatisticsUtils.waitStatisticalInfoCount( threadNum );
        queryStatisticInfoAndCheck( threadNum );
        // 清理统计表中统计信息
        StatisticsUtils.clearStatisticalInfo();
        actSuccessCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( actSuccessCount.get() == generateData().length
                    || TestScmBase.forceClear ) {
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

    private class CreateFile {
        @ExecuteOrder(step = 1)
        private void create() throws ScmException {
            ScmConfigOption scOpt = new ScmConfigOption(
                    gateWayUrl + "/" + site.getSiteServiceName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            ScmSession session = null;
            try {
                session = ScmFactory.Session.createSession(
                        ScmType.SessionType.AUTH_SESSION, scOpt );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileNameBase + UUID.randomUUID() );
                file.setAuthor( fileNameBase );
                fileIdList.add( file.save() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}