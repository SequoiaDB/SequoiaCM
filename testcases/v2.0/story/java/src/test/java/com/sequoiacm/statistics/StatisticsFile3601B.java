package com.sequoiacm.statistics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.bson.BSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
import com.sequoiacm.client.element.ScmFileStatisticsType;
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
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3601:有多个网关节点/amdinserver节点，请求信息被持久化后，查询统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3601B extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private Calendar calendar = null;
    private String fileNameBase = "file3601B";
    private int fileNum = 50;
    private List< ScmId > fileIdList = new ArrayList<>();
    private AtomicLong totalTime = new AtomicLong( 0 );
    private int fileSize = 1024 * 5;
    private Date beginDate = null;
    private Date endDate = null;

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        if ( TestScmBase.gateWayList.size() < 2 ) {
            throw new SkipException( "网关节点少于2个,跳过此用例!!!" );
        }
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        prepareEnv();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ThreadExecutor threadExecutor = new ThreadExecutor();
        for ( int i = 0; i < fileNum; i++ ) {
            threadExecutor.addWorker( new DownloadFile( fileIdList.get( i ) ) );
        }
        threadExecutor.run();
        StatisticsUtils.waitStatisticalInfoCount( fileNum );

        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) - 1 );
        beginDate = calendar.getTime();
        // 查询下载接口统计信息
        ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate )
                .workspace( wsp.getName() ).timeAccuracy( ScmTimeAccuracy.DAY )
                .download().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), ScmTimeAccuracy.DAY,
                fileNum, fileSize, totalTime.get() / fileNum );
        StatisticsUtils.checkScmFileStatisticInfo( downloadInfo,
                expDownloadInfo );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
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

    private void prepareEnv() throws Exception {
        // 清理环境
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        // 创建文件
        byte[] bytes = new byte[ fileSize ];
        new Random().nextBytes( bytes );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + UUID.randomUUID() );
            file.setAuthor( fileNameBase );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList.add( file.save() );
        }
        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download" );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "120" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1000" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    private class DownloadFile {
        private ScmId fileId;

        public DownloadFile( ScmId fileId ) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void download() throws ScmException {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( new Random()
                            .nextInt( TestScmBase.gateWayList.size() ) ) + "/"
                            + site.getSiteServiceName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            ScmSession session = null;
            try {
                session = ScmFactory.Session.createSession(
                        ScmType.SessionType.AUTH_SESSION, scOpt );
                long beginTime = System.currentTimeMillis();
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                // download file
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                file.getContent( outputStream );
                totalTime.addAndGet( System.currentTimeMillis() - beginTime );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}