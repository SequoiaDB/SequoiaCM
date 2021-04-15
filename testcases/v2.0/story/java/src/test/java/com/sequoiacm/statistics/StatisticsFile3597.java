package com.sequoiacm.statistics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
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
 * @Description: SCM-3597:覆盖所有下载接口，查询上传接口的统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3597 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private Calendar calendar = Calendar.getInstance();
    private String fileName = "file3597";
    private int fileSize = 200 * 1024;
    private int requestCount = 0;
    private int totalFileSize = 0;
    private long totalDownloadTime = 0;
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
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        createFile();
        downloadFile();
        StatisticsUtils.waitStatisticalInfoCount( requestCount );
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
                requestCount, totalFileSize / requestCount,
                totalDownloadTime / requestCount );
        StatisticsUtils.checkScmFileStatisticInfo( downloadInfo,
                expDownloadInfo );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                StatisticsUtils.clearStatisticalInfo();
            }
        } finally {
            ConfUtil.deleteGateWayStatisticalConf();
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void downloadFile() throws ScmException {
        long downloadBeginTime = System.currentTimeMillis();
        // 正常下载文件
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        file1.getContent( new ByteArrayOutputStream() );
        totalFileSize += fileSize;
        requestCount++;

        // 通过路径获取文件，下载文件内容
        ScmFile file2 = ScmFactory.File.getInstanceByPath( ws, "/" + fileName );
        file2.getContent( new ByteArrayOutputStream() );
        totalFileSize += fileSize;
        requestCount++;

        // 通过获取子文件，下载文件内容
        ScmFile file3 = ScmFactory.Directory.getInstance( ws, "/" )
                .getSubfile( fileName );
        file3.getContent( new ByteArrayOutputStream() );
        totalFileSize += fileSize;
        requestCount++;

        // seek文件
        ScmFile file4 = ScmFactory.File.getInstance( ws, fileId );
        ScmInputStream in = ScmFactory.File
                .createInputStream( ScmType.InputStreamType.SEEKABLE, file4 );
        in.seek( CommonDefine.SeekType.SCM_FILE_SEEK_SET, fileSize / 2 );
        in.read( new ByteArrayOutputStream() );
        totalFileSize += fileSize / 2;
        requestCount++;

        in.seek( CommonDefine.SeekType.SCM_FILE_SEEK_SET, fileSize / 3 );
        in.read( new ByteArrayOutputStream() );
        totalFileSize += 2 * fileSize / 3;
        requestCount++;
        totalDownloadTime = System.currentTimeMillis() - downloadBeginTime;
    }

    private void createFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        byte[] bytes = new byte[ fileSize ];
        new Random().nextBytes( bytes );
        file.setContent( new ByteArrayInputStream( bytes ) );
        fileId = file.save();
    }

    private void prepareEnv() throws Exception {
        // 清理环境
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download" );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }
}