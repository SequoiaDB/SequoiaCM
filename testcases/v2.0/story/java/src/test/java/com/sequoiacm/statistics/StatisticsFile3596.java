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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @Description: SCM-3596:指定下载文件大小，经过rawDataReportPeriod周期后查询下载接口的统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3596 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmWorkspace ws = null;
    private ScmSession session = null;
    private String fileNameBase = "file3596";
    private List< ScmId > fileIdList = new ArrayList<>();
    private int[] fileSizes = { 0, 10 * 1024 * 1023, 11 * 1024 * 1024 };
    private int fileNum = fileSizes.length;
    private int totalDownloadTime = 0;
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理环境和更新配置
        prepareEnv();
        // 制造下载请求信息
        totalDownloadTime += downloadFile();
        StatisticsUtils.waitStatisticalInfoCount( fileNum );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - fileNum * 5 );
        beginDate = calendar.getTime();
        // 查询下载接口统计信息
        ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .workspace( wsp.getName() ).beginDate( beginDate )
                .endDate( endDate ).download().get();
        // 检查结果
        long totalSize = 0;
        for ( int i = 0; i < fileSizes.length; i++ ) {
            totalSize += fileSizes[ i ];
        }
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), null, fileNum,
                totalSize / fileNum, totalDownloadTime / fileNum );
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
        createFiles();
        StatisticsUtils.clearStatisticalInfo();
    }

    private void createFiles() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
            file.setAuthor( fileNameBase );
            byte[] bytes = new byte[ fileSizes[ i ] ];
            new Random().nextBytes( bytes );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList.add( file.save() );
        }
    }

    private long downloadFile() throws Exception {
        long downloadBeginTime = System.currentTimeMillis();
        for ( ScmId fileId : fileIdList ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            // download file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            file.getContent( outputStream );
        }
        return System.currentTimeMillis() - downloadBeginTime;
    }
}