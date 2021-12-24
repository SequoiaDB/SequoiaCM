package com.sequoiacm.statistics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @description SCM-3589:指定时间段内有统计信息，用户查询单个工作区上传/下载接口的统计信息
 *              SCM-3639:循环查询文件统计信息，查看链接是否泄漏
 * @author fanyu
 * @createDate 2021.03.30
 * @updateUser ZhangYanan
 * @updateDate 2021.12.24
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile3589 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String fileNameBase = "file3589";
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private int fileSize = 200 * 1024;
    private int fileNum = 10;
    private int totalUploadTime = 0;
    private int totalDownloadTime = 0;
    private Calendar calendar = Calendar.getInstance();
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        // 清理环境和更新配置
        prepareEnv();
        // 制造上传和下载请求信息
        prepareRawData();
        StatisticsUtils.waitStatisticalInfoCount( fileNum * 2 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        calendar.set( Calendar.HOUR_OF_DAY,
                calendar.get( Calendar.HOUR_OF_DAY ) + 1 );
        Date endDate = calendar.getTime();
        calendar.set( Calendar.HOUR_OF_DAY,
                calendar.get( Calendar.HOUR_OF_DAY ) - 10 );
        Date beginDate = calendar.getTime();
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            // 查询上传接口统计信息
            for ( int i = 0; i < 260; i++ ) {
                ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                        .fileStatistician( session )
                        .user( TestScmBase.scmUserName ).beginDate( beginDate )
                        .endDate( endDate ).timeAccuracy( ScmTimeAccuracy.HOUR )
                        .workspace( wsp.getName() ).upload().get();
                // 检查结果
                ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                        ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                        TestScmBase.scmUserName, wsp.getName(),
                        ScmTimeAccuracy.HOUR, fileNum, fileSize,
                        totalUploadTime / fileNum );
                StatisticsUtils.checkScmFileStatisticInfo( uploadInfo,
                        expUploadInfo );
            }

            // 查询下载接口统计信息
            for ( int i = 0; i < 260; i++ ) {
                ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                        .fileStatistician( session )
                        .user( TestScmBase.scmUserName )
                        .timeAccuracy( ScmTimeAccuracy.HOUR )
                        .beginDate( beginDate ).endDate( endDate )
                        .workspace( wsp.getName() ).download().get();
                ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                        ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                        TestScmBase.scmUserName, wsp.getName(),
                        ScmTimeAccuracy.HOUR, fileNum, fileSize,
                        totalDownloadTime / fileNum );
                StatisticsUtils.checkScmFileStatisticInfo( downloadInfo,
                        expDownloadInfo );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmSession session = null;
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                session = TestScmTools.createSession( site );
                TestTools.LocalFile.removeFile( localPath );
                ScmWorkspace ws1 = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // 清理统计表
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws1, fileId, true );
                }
            }
        } finally {
            StatisticsUtils.restoreGateWaySystemTime();
            ConfUtil.deleteGateWayStatisticalConf();
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareRawData() throws Exception {
        // ws只有1条上传统计信息，有多条下载统计信息
        totalUploadTime += createFiles( wsp.getName(), fileNum, fileIdList1 );
        // 网关时间跳变，制造有多条记录
        // 打印日历时间，定位ci问题
        System.out.println( "----calendar.getTime=" + calendar.getTime() );
        System.out.println(
                "----calendar.getTimeInMillis=" + calendar.getTimeInMillis() );
        totalDownloadTime += downloadFile( wsp.getName(),
                fileIdList1.subList( 0, fileNum / 2 ),
                calendar.getTimeInMillis() );
        calendar.set( Calendar.HOUR_OF_DAY,
                calendar.get( Calendar.HOUR_OF_DAY ) + 1 );
        // 打印日历时间，定位ci问题
        System.out.println( "----calendar.getTime=" + calendar.getTime() );
        System.out.println(
                "----calendar.getTimeInMillis=" + calendar.getTimeInMillis() );
        totalDownloadTime += downloadFile( wsp.getName(),
                fileIdList1.subList( fileNum / 2, fileNum ),
                calendar.getTimeInMillis() );
    }

    private void prepareEnv() throws Exception {
        // 清理环境
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download,file_upload" );
        confMap1.put( "scm.statistics.rawDataCacheSize", "50" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "HOUR" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    private long createFiles( String wsName, int fileNum,
            List< ScmId > fileIdList ) throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
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
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            long downloadBeginTime = System.currentTimeMillis();
            for ( ScmId fileId : fileIdList ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                // down file
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