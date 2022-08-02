package com.sequoiacm.statistics;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
public class StatisticsFile3601A extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private Calendar calendar = null;
    private String fileNameBase = "file3601A";
    private int fileNum = 50;
    private List< ScmId > fileIdList = new CopyOnWriteArrayList<>();
    private AtomicLong totalTime = new AtomicLong( 0 );
    private int fileSize = 1024 * 300;
    private byte[] bytes = new byte[ fileSize ];
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
        new Random().nextBytes( bytes );
        prepareEnv();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ThreadExecutor threadExecutor = new ThreadExecutor();
        for ( String gateWayUrl : TestScmBase.gateWayList ) {
            threadExecutor.addWorker( new CreateFile( gateWayUrl ) );
        }
        threadExecutor.run();
        StatisticsUtils.waitStatisticalInfoCount( fileNum );

        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) - 1 );
        beginDate = calendar.getTime();
        // 查询下载接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate )
                .workspace( wsp.getName() ).timeAccuracy( ScmTimeAccuracy.DAY )
                .upload().get();
        // 检查结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), ScmTimeAccuracy.DAY,
                fileNum, fileSize, totalTime.get() / fileNum );
        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo, expUploadInfo );
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

        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "50" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1000" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    private class CreateFile {
        private String gateWayUrl;

        public CreateFile( String gateWayUrl ) {
            this.gateWayUrl = gateWayUrl;
        }

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
                long beginTime = System.currentTimeMillis();
                for ( int i = 0; i < fileNum
                        / TestScmBase.gateWayList.size(); i++ ) {
                    ScmFile file = ScmFactory.File.createInstance( ws );
                    file.setFileName( fileNameBase + UUID.randomUUID() );
                    file.setAuthor( fileNameBase );
                    file.setContent( new ByteArrayInputStream( bytes ) );
                    fileIdList.add( file.save() );
                }
                totalTime.addAndGet( System.currentTimeMillis() - beginTime );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}