package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;

import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @description SCM-4036 : 文件关联批次上传下载，查询统计信息
 * @author ZhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile4036 extends TestScmBase {
    private File localPath = null;
    private int uploadFilesSuccedNums = 7;
    private int downloadFilesSuccedNums = 13;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private String fileName = "file4036";
    private String batchName1 = "batch14036";
    private String batchName2 = "batch24036";
    private ScmSession siteSession = null;
    private ScmWorkspace siteWorkspace = null;
    private int[] fileSizes = { 200001 * 1024, 10000, 1024, 20000, 700 * 1024,
            300 * 1024, 5, 100 * 1024, 1024 * 1024 * 11, 800 * 1024, 900 * 1024,
            10 * 1024, 50 * 1024, 90 * 1024, 60 * 1024, 2024 * 2024, 6 * 1024,
            700 * 1024, 99, 6 };
    private List< Integer > uploadTime = new ArrayList<>();
    private List< Integer > downloadTime = new ArrayList<>();
    private List< ScmFile > filesList = new ArrayList<>();
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;
    private List< String > filePathList = new ArrayList<>();
    private List< ScmId > batchIdList = new ArrayList<>();

    @BeforeClass
    public void setUp() throws Exception {
        calendar = Calendar.getInstance();
        localPath = ScmFileUtils.createFiles( fileSizes, filePathList );
        wsp = ScmInfo.getWs();
        site = ScmInfo.getSite();
        siteSession = ScmSessionUtils.createSession( site );
        siteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                siteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        uploadFileRelevancyBatch( batchName2, uploadFilesSuccedNums,
                uploadFilesSuccedNums + downloadFilesSuccedNums );
        // 更新网关和admin配置
        StatisticsUtils.configureGatewayAndAdminInfo( wsp );
        // 设置统计起始时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        beginDate = calendar.getTime();

        // 制造上传下载请求信息
        constructStatisticsInfo();
        StatisticsUtils.waitStatisticalInfoCount(
                uploadFilesSuccedNums + downloadFilesSuccedNums );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 设置查询截止时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();
        // 查询接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( scmUserName )
                .workspace( wsp.getName() ).beginDate( beginDate )
                .endDate( endDate ).upload().get();

        ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( scmUserName )
                .workspace( wsp.getName() ).beginDate( beginDate )
                .endDate( endDate ).download().get();

        checkScmFileStatInfo( uploadInfo, downloadInfo, uploadTime,
                downloadTime, uploadFilesSuccedNums, downloadFilesSuccedNums );
        runSuccess = true;
    }

    @AfterClass()
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId batchId : batchIdList ) {
                    ScmFactory.Batch.deleteInstance( siteWorkspace, batchId );
                }
            } finally {
                ConfUtil.deleteGateWayStatisticalConf();
                if ( siteSession != null ) {
                    siteSession.close();
                }
            }
        }
    }

    public static void checkScmFileStatisticInfo( ScmFileStatisticInfo actInfo,
            ScmFileStatisticInfo expInfo ) throws Exception {
        Assert.assertEquals( actInfo.getFailCount(), expInfo.getFailCount() );
        Assert.assertEquals( actInfo.getUser(), expInfo.getUser() );
        Assert.assertEquals( actInfo.getSuccessCount(),
                expInfo.getSuccessCount() );
        Assert.assertEquals( actInfo.getMaxResponseTime() > 0
                && actInfo.getMaxResponseTime() <= expInfo.getMaxResponseTime(),
                true );
        Assert.assertEquals( actInfo.getMinResponseTime() > 0
                && actInfo.getMinResponseTime() <= expInfo.getMinResponseTime(),
                true );
    }

    public void constructStatisticsInfo() throws Exception {
        uploadFileRelevancyBatch( batchName1, 0, uploadFilesSuccedNums );
        downloadFileRelevancyBatch();
    }

    public void checkScmFileStatInfo( ScmFileStatisticInfo uploadInfo,
            ScmFileStatisticInfo downloadInfo, List< Integer > uploadTime,
            List< Integer > downloadTime, int upSuccesCount,
            int downSuccesCount ) throws Exception {
        // 取最大响应时间和最小响应时间
        long upFileMaxTime = Collections.max( uploadTime );
        long upFileMinTime = Collections.min( uploadTime );

        long downFileMaxTime = Collections.max( downloadTime );
        long downFileMinTime = Collections.min( downloadTime );

        // 检查结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo( null,
                null, null, scmUserName, null, null, upSuccesCount, 0, 0,
                upFileMaxTime, upFileMinTime, 0 );

        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo( null,
                null, null, scmUserName, null, null, downSuccesCount, 0, 0,
                downFileMaxTime, downFileMinTime, 0 );

        checkScmFileStatisticInfo( uploadInfo, expUploadInfo );
        checkScmFileStatisticInfo( downloadInfo, expDownloadInfo );
    }

    public void uploadFileRelevancyBatch( String batchName, int beginNo,
            int endNo ) throws Exception {
        // 创建批次"Batch"，并设置自定义标签
        ScmBatch batch1 = ScmFactory.Batch.createInstance( siteWorkspace );
        // 创建自定义ID批次
        ScmTags tags = new ScmTags();
        tags.addTag( "tagValue" );
        batch1.setTags( tags );
        batch1.setName( batchName );
        batchIdList.add( batch1.save() );
        // 上传文件后关联批次
        for ( int i = beginNo; i < endNo; i++ ) {
            int totalUploadTime = ( int ) uploadFile( filePathList.get( i ),
                    fileName, batch1, siteWorkspace );
            uploadTime.add( totalUploadTime );
        }
    }

    public void downloadFileRelevancyBatch() throws Exception {
        // 下载批次下文件
        ScmBatch batch2 = ScmFactory.Batch.getInstance( siteWorkspace,
                batchIdList.get( 0 ) );
        filesList = batch2.listFiles();
        for ( int i = 0; i < filesList.size(); i++ ) {
            ScmId fileId = filesList.get( i ).getFileId();
            int totalDownloadTime = ( int ) ScmFileUtils
                    .downloadFile( fileId, siteWorkspace );
            downloadTime.add( totalDownloadTime );
        }
    }

    public long uploadFile( String filePath, String fileName, ScmBatch batch,
            ScmWorkspace siteWorkspace ) throws Exception {
        long downloadBeginTime = System.currentTimeMillis();
        ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
        file.setFileName( fileName + UUID.randomUUID() );
        file.setAuthor( fileName );
        file.setContent( filePath );
        batch.attachFile( file.save() );
        return System.currentTimeMillis() - downloadBeginTime;
    }
}
