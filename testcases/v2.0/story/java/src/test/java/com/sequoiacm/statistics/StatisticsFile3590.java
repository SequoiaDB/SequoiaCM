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
 * @Description: SCM-3590:指定时间段内无统计信息，其它时间段有统计信息，用户查询上传/下载接口的统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3590 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmWorkspace ws;
    private ScmSession session = null;
    private String fileNameBase = "file3590";
    private List< ScmId > fileIdList = new ArrayList<>();
    private int fileSize = 200 * 1024;
    private int fileNum = 10;
    private Calendar calendar = null;
    private Date beginDate = null;
    private Date endDate = null;
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
        calendar = Calendar.getInstance();
        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理环境和更新配置
        prepareEnv();
        // 制造上传和下载请求信息
        createFiles();
        downloadFile();
        // 确认统计表中有ws的文件统计信息
        StatisticsUtils.waitStatisticalInfoCount( fileNum * 2 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 2 );
        beginDate = calendar.getTime();

        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate )
                .workspace( wsp.getName() ).upload().get();
        // 检查结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), null, 0, 0, 0 );
        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo, expUploadInfo );

        // 查询上传接口统计信息
        ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate )
                .workspace( wsp.getName() ).download().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), null, 0, 0, 0 );
        StatisticsUtils.checkScmFileStatisticInfo( downloadInfo,
                expDownloadInfo );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
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

    private void prepareEnv() throws Exception {
        // 清理环境
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        ConfUtil.deleteGateWayStatisticalConf();

        // 更新网关配置
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_upload,file_download" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
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

    private void createFiles() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
            file.setAuthor( fileNameBase );
            file.setContent( filePath );
            fileIdList.add( file.save() );
        }
    }

    private void downloadFile() throws Exception {
        for ( ScmId fileId : fileIdList ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            // download file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            file.getContent( outputStream );
        }
    }
}