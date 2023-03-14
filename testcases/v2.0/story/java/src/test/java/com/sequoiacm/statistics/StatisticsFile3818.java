package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @description SCM-3818:不同工作空间下，文件上传部分失败，查询统计信息。
 * @author ZhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile3818 extends TestScmBase {
    private File localPath = null;
    private WsWrapper wsp1 = null;
    private WsWrapper wsp2 = null;
    private int uploadFilesFaidNums1 = 6;
    private int uploadFilesFaidNums2 = 7;
    private int uploadFilesSuccedNums1 = 4;
    private int uploadFilesSuccedNums2 = 3;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private BSONObject queryCond = null;
    private String fileName = "file3818";
    private ScmSession siteSession1 = null;
    private ScmSession siteSession2 = null;
    private ScmWorkspace siteWorkspace1 = null;
    private ScmWorkspace siteWorkspace2 = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private int[] fileSizes = { 200001 * 1024, 10000, 1024, 20000, 700 * 1024,
            300 * 1024, 5, 100 * 1024, 1024 * 1024 * 11, 800 * 1024, 900 * 1024,
            10 * 1024, 50 * 1024, 90 * 1024, 60 * 1024, 2024 * 2024, 6 * 1024,
            700 * 1024, 99, 6 };
    private int fileNums = 0;
    private List< Integer > uploadTime1 = new ArrayList<>();
    private List< Integer > uploadTime2 = new ArrayList<>();
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;
    private List< String > filePathList = new ArrayList<>();

    @BeforeClass
    public void setUp() throws Exception {
        calendar = Calendar.getInstance();
        if ( ScmInfo.getWsNum() < 2 ) {
            throw new SkipTestException( "need 2 wss!!!!" );
        }
        fileNums = fileSizes.length;
        localPath = ScmFileUtils.createFiles( fileSizes, filePathList );

        site = ScmInfo.getSite();
        wsp1 = ScmInfo.getAllWorkspaces().get( 0 );
        wsp2 = ScmInfo.getAllWorkspaces().get( 1 );

        siteSession1 = ScmSessionUtils.createSession( site );
        siteSession2 = ScmSessionUtils.createSession( site );
        siteWorkspace1 = ScmFactory.Workspace.getWorkspace( wsp1.getName(),
                siteSession1 );
        siteWorkspace2 = ScmFactory.Workspace.getWorkspace( wsp2.getName(),
                siteSession2 );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp1, queryCond );
        ScmFileUtils.cleanFile( wsp2, queryCond );
        // 更新网关和admin配置
        prepareEnv();
        // 设置统计起始时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        beginDate = calendar.getTime();
        // 制造上传请求信息
        constructStatisticsInfo();
        StatisticsUtils.waitStatisticalInfoCount( fileNums );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        // 设置查询截至时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();
        checkScmFileStatInfo( siteSession1, uploadTime1, uploadFilesFaidNums1,
                wsp1 );
        checkScmFileStatInfo( siteSession2, uploadTime2, uploadFilesFaidNums2,
                wsp2 );
        runSuccess = true;
    }

    @AfterClass()
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp1, queryCond );
                ScmFileUtils.cleanFile( wsp2, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                ConfUtil.deleteGateWayStatisticalConf();
                if ( siteSession1 != null ) {
                    siteSession1.close();
                }
                if ( siteSession2 != null ) {
                    siteSession2.close();
                }
            }
        }
    }

    public void checkScmFileStatisticInfo( ScmFileStatisticInfo actInfo,
            ScmFileStatisticInfo expInfo ) throws Exception {
        try {
            Assert.assertEquals( actInfo.getFailCount(),
                    expInfo.getFailCount() );
            Assert.assertEquals( actInfo.getWorkspace(),
                    expInfo.getWorkspace() );
            Assert.assertEquals( actInfo.getSuccessCount(),
                    expInfo.getSuccessCount() );
            Assert.assertEquals( actInfo.getMaxResponseTime() >= 0 && actInfo
                    .getMaxResponseTime() <= expInfo.getMaxResponseTime(),
                    true );
            Assert.assertEquals( actInfo.getMinResponseTime() >= 0 && actInfo
                    .getMinResponseTime() <= expInfo.getMinResponseTime(),
                    true );
        } catch ( AssertionError e ) {
            throw new Exception( "act = " + actInfo.toString() + "\n,exp = "
                    + expInfo.toString(), e );
        }
    }

    public void prepareEnv() throws Exception {
        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp1.getName() + "," + wsp2.getName() );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    public void constructStatisticsInfo() throws Exception {
        // user1上传文件
        for ( int i = 0; i < uploadFilesSuccedNums1; i++ ) {
            int totalUploadTime = ( int ) ScmFileUtils.createFiles(
                    filePathList.get( i ), fileName, fileIdList,
                    siteWorkspace1 );
            uploadTime1.add( totalUploadTime );
        }
        int count = uploadFilesSuccedNums1 + uploadFilesFaidNums1;
        for ( int i = uploadFilesSuccedNums1; i < count; i++ ) {
            ScmFileUtils.createFileFialed( filePathList.get( i ), fileName,
                    fileIdList, siteWorkspace1 );
        }
        // user2上传文件
        for ( int i = count; i < count + uploadFilesSuccedNums2; i++ ) {
            int totalUploadTime = ( int ) ScmFileUtils.createFiles(
                    filePathList.get( i ), fileName, fileIdList,
                    siteWorkspace2 );
            uploadTime2.add( totalUploadTime );
        }

        for ( int i = count + uploadFilesSuccedNums2; i < fileNums; i++ ) {
            ScmFileUtils.createFileFialed( filePathList.get( i ), fileName,
                    fileIdList, siteWorkspace2 );
        }
    }

    public void checkScmFileStatInfo( ScmSession siteSession,
            List< Integer > uploadTime, int failcount, WsWrapper wsp )
            throws Exception {
        // 取最大响应时间和最小响应时间
        long maxTime = Collections.max( uploadTime );
        long minTime = Collections.min( uploadTime );

        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( TestScmBase.scmUserName )
                .workspace( wsp.getName() ).beginDate( beginDate )
                .endDate( endDate ).upload().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo( null,
                null, null, TestScmBase.scmUserName, wsp.getName(), null,
                fileNums / 2, 0, 0, maxTime, minTime, failcount );
        checkScmFileStatisticInfo( uploadInfo, expDownloadInfo );
    }
}
