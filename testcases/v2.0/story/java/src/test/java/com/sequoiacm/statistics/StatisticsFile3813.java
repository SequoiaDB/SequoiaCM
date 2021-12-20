package com.sequoiacm.statistics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @description SCM-3813:下载文件全部失败，查询最大响应时间、最小响应时间和失败请求数
 * @author ZhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile3813 extends TestScmBase {
    private File localPath = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private String fileName = "file3813";
    private ScmSession siteSession = null;
    private ScmWorkspace siteWorkspace = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private int[] fileSizes = { 200001 * 1024, 10000, 1024, 20000, 700 * 1024,
            300 * 1024, 5, 100 * 1024, 1024 * 1024 * 11, 800 * 1024, 900 * 1024,
            10 * 1024, 50 * 1024, 90 * 1024, 60 * 1024, 2024 * 2024, 6 * 1024,
            700 * 1024, 99, 6 };
    private int fileNums = 0;
    private List< Integer > downloadTime = new ArrayList<>();
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = Calendar.getInstance();
    private List< String > filePathList = new ArrayList<>();

    @BeforeClass
    public void setUp() throws Exception {
        fileNums = fileSizes.length;
        localPath = StatisticsUtils.createFile( fileSizes, filePathList );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        siteSession = TestScmTools.createSession( site );
        siteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                siteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        for ( int i = 0; i < fileNums; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
            file.setFileName( fileName + i );
            file.setAuthor( fileName );
            file.setContent( filePathList.get( i ) );
            fileIdList.add( file.save() );
        }
        // 更新网关和admin配置
        ScmFileStatisticsType statisticType = ScmFileStatisticsType.FILE_DOWNLOAD;
        StatisticsUtils.configureGatewayAndAdminInfo( wsp, statisticType );
        // 设置统计起始时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        beginDate = calendar.getTime();
        // 制造下载请求信息
        constructStatisticsInfo();
        StatisticsUtils.waitStatisticalInfoCount( fileNums );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        // 设定统计结束时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();

        // 查询下载接口统计信息
        ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate ).download().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_DOWNLOAD, null, null,
                TestScmBase.scmUserName, null, null, fileNums, 0, 0, 0, 0,
                fileNums );
        StatisticsUtils.checkScmFileStatisticNewAddInfo( downloadInfo,
                expDownloadInfo );
        runSuccess = true;
    }

    @AfterClass()
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                ConfUtil.deleteGateWayStatisticalConf();
                if ( siteSession != null ) {
                    siteSession.close();
                }
            }
        }
    }

    public void constructStatisticsInfo() throws Exception {
        // 有多条下载信息
        for ( ScmId fileID : fileIdList ) {
            StatisticsUtils.downloadFileFialed( fileID, siteWorkspace );
        }
    }
}