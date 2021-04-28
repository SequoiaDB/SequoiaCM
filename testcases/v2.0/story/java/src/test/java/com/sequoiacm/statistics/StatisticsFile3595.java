package com.sequoiacm.statistics;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
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

/**
 * @Description: SCM-3595:未统计该工作区上传接口请求信息，查询该工作区上传接口的统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3595 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private Calendar calendar = Calendar.getInstance();
    private String fileName = "file3595";
    private int totalDownloadTime = 0;
    private ScmId fileId = null;
    private Date beginDate = null;
    private Date endDate = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理统计信息
        StatisticsUtils.clearStatisticalInfo();
        prepareEnv();
        totalDownloadTime += downloadFile();
        StatisticsUtils.waitStatisticalInfoCount( 1 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) - 1 );
        beginDate = calendar.getTime();

        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate )
                .workspace( ws.getName() ).timeAccuracy( ScmTimeAccuracy.DAY )
                .upload().get();
        // 检查结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), ScmTimeAccuracy.DAY, 0,
                0, 0 );
        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo, expUploadInfo );

        // 查询下载接口统计信息
        ScmFileStatisticInfo downLoadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate )
                .workspace( wsp.getName() ).timeAccuracy( ScmTimeAccuracy.DAY )
                .download().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), ScmTimeAccuracy.DAY, 1,
                0, totalDownloadTime );
        StatisticsUtils.checkScmFileStatisticInfo( downLoadInfo,
                expDownloadInfo );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
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
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        ConfUtil.deleteGateWayStatisticalConf();
        // 更新网关配置
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download" );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1000" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );

        // 创建文件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        fileId = file.save();

    }

    private long downloadFile() throws ScmException {
        long downloadBeginTime = System.currentTimeMillis();
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        // download file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        return System.currentTimeMillis() - downloadBeginTime;
    }
}