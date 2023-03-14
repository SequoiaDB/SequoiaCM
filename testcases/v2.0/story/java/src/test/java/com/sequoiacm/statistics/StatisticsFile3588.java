package com.sequoiacm.statistics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @Description: SCM-3588:指定时间段内有统计信息，用户查询所有工作区上传/下载接口的统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3588 extends TestScmBase {
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );
    private SiteWrapper site = null;
    private WsWrapper wsp1 = null;
    private WsWrapper wsp2 = null;
    private String fileNameBase = "file3588";
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private int fileSize = 200 * 1024;
    private int fileNums = 10;
    private int totalUploadTime = 0;
    private int totalDownloadTime = 0;
    private Calendar calendar = null;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        if ( ScmInfo.getWsNum() < 2 ) {
            throw new SkipTestException( "need 2 wss!!!!" );
        }
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        site = ScmInfo.getSite();
        List< WsWrapper > wsList = ScmInfo.getAllWorkspaces();
        wsp1 = wsList.get( 0 );
        wsp2 = wsList.get( 1 );
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            // 清理环境和更新配置
            prepareEnv();
            // 制造上传和下载请求信息
            prepareRawData();
            StatisticsUtils.waitStatisticalInfoCount( fileNums * 4 );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @DataProvider(name = "dataProvider")
    public Object[][] generateDate() {
        // 跨天
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        Date endDate1 = calendar.getTime();
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 10 );
        Date beginDate1 = calendar.getTime();

        // 跨月
        calendar.set( Calendar.MONTH, calendar.get( Calendar.MONTH ) + 1 );
        Date endDate2 = calendar.getTime();
        calendar.set( Calendar.MONTH, calendar.get( Calendar.MONTH ) - 10 );
        Date beginDate2 = calendar.getTime();

        // 跨年
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) + 1 );
        Date endDate3 = calendar.getTime();
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) - 10 );
        Date beginDate3 = calendar.getTime();

        return new Object[][] { { beginDate1, endDate1 },
                { beginDate2, endDate2 }, { beginDate3, endDate3 } };
    }

    @Test(dataProvider = "dataProvider")
    private void test( Date beginDate, Date endDate ) throws Exception {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            // 查询上传接口统计信息
            ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                    .fileStatistician( session ).user( TestScmBase.scmUserName )
                    .beginDate( beginDate ).endDate( endDate )
                    .timeAccuracy( ScmTimeAccuracy.DAY ).upload().get();
            // 检查结果
            ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                    ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                    TestScmBase.scmUserName, null, ScmTimeAccuracy.DAY,
                    fileNums * 2, fileSize,
                    totalUploadTime / ( fileNums * 2 ) );
            StatisticsUtils.checkScmFileStatisticInfo( uploadInfo,
                    expUploadInfo );

            // 查询下载接口统计信息
            ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                    .fileStatistician( session ).user( TestScmBase.scmUserName )
                    .beginDate( beginDate ).endDate( endDate )
                    .timeAccuracy( ScmTimeAccuracy.DAY ).download().get();
            ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                    ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                    TestScmBase.scmUserName, null, ScmTimeAccuracy.DAY,
                    fileNums * 2, fileSize,
                    totalDownloadTime / ( fileNums * 2 ) );
            StatisticsUtils.checkScmFileStatisticInfo( downloadInfo,
                    expDownloadInfo );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        runSuccessCount.incrementAndGet();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmSession session = null;
        try {
            if ( runSuccessCount.get() == generateDate().length
                    || TestScmBase.forceClear ) {
                session = ScmSessionUtils.createSession( site );
                TestTools.LocalFile.removeFile( localPath );
                ScmWorkspace ws1 = ScmFactory.Workspace
                        .getWorkspace( wsp1.getName(), session );
                ScmWorkspace ws2 = ScmFactory.Workspace
                        .getWorkspace( wsp2.getName(), session );
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws1, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws2, fileId, true );
                }
            }
        } finally {
            if ( session != null ) {
                StatisticsUtils.restoreGateWaySystemTime();
                ConfUtil.deleteGateWayStatisticalConf();
                session.close();
            }
        }
    }

    private void prepareRawData() throws Exception {
        // ws1只有1条上传统计信息，有多条下载统计信息
        totalUploadTime += createFiles( wsp1.getName(), fileNums,
                calendar.getTimeInMillis(), fileIdList1 );
        totalDownloadTime += downloadFile( wsp1.getName(),
                fileIdList1.subList( 0, fileNums / 2 ),
                calendar.getTimeInMillis() );
        // 网关时间跳变，制造有多条记录
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        totalDownloadTime += downloadFile( wsp1.getName(),
                fileIdList1.subList( fileNums / 2, fileNums ),
                calendar.getTimeInMillis() );

        // ws2有多条上传统计信息，有一条下载统计信息
        totalUploadTime += createFiles( wsp2.getName(), fileNums / 2,
                calendar.getTimeInMillis(), fileIdList2 );
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        totalUploadTime += createFiles( wsp2.getName(), fileNums / 2,
                calendar.getTimeInMillis(), fileIdList2 );

        totalDownloadTime += downloadFile( wsp2.getName(), fileIdList2,
                calendar.getTimeInMillis() );
    }

    private void prepareEnv() throws Exception {
        // 清理环境
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmFileUtils.cleanFile( wsp1, cond );
        ScmFileUtils.cleanFile( wsp2, cond );
        ConfUtil.deleteGateWayStatisticalConf();
        // 更新网关配置
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download,file_upload" );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaces",
                wsp1.getName() + "," + wsp2.getName() );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp1.getName() + "," + wsp2.getName() );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    private long createFiles( String wsName, int fileNum, long gateWayLocalTime,
            List< ScmId > fileIdList ) throws Exception {
        StatisticsUtils.setGateWaySystemTime( gateWayLocalTime );
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            long uploadBeginTime = System.currentTimeMillis();
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
                file.setAuthor( fileNameBase );
                file.setContent( filePath );
                fileIdList.add( file.save() );
            }
            return System.currentTimeMillis() - uploadBeginTime;
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private long downloadFile( String wsName, List< ScmId > fileIdList,
            long gateWayLocalTime ) throws Exception {
        StatisticsUtils.setGateWaySystemTime( gateWayLocalTime );
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            long downloadBeginTime = System.currentTimeMillis();
            for ( ScmId fileId : fileIdList ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                // download file
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                file.getContent( outputStream );
            }
            return System.currentTimeMillis() - downloadBeginTime;
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}