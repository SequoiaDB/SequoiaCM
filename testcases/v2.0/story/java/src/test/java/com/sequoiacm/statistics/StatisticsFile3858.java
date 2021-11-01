package com.sequoiacm.statistics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
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

/**
 * @description SCM-3858:文件上传下载并发，查询最大响应时间、最小响应时间和失败请求数
 * @author ZhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile3858 extends TestScmBase {
    private File localPath = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private String fileName = "file3858";
    private ScmSession siteSession = null;
    private ScmWorkspace siteWorkspace = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private List< Integer > downloadTime = new ArrayList<>();
    private List< Integer > uploadTime = new ArrayList<>();
    private int[] fileSizes = { 200001 * 1024, 10000, 1024, 20000, 700 * 1024,
            300 * 1024, 5, 100 * 1024, 1024 * 1024 * 11, 800 * 1024, 900 * 1024,
            10 * 1024, 50 * 1024, 90 * 1024, 60 * 1024, 2024 * 2024, 6 * 1024,
            700 * 1024, 99, 6 };
    private int fileNums = 0;
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = Calendar.getInstance();
    private List< String > filePathList = new ArrayList<>();

    @BeforeClass
    public void setUp() throws Exception {
        fileNums = fileSizes.length;
        localPath = StatisticsUtils.createFile( fileSizes, filePathList );

        site = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        siteSession = TestScmTools.createSession( site );
        siteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                siteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        for ( int i = 0; i < fileNums / 2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
            file.setFileName( fileName + i );
            file.setAuthor( fileName );
            file.setContent( filePathList.get( i ) );
            fileIdList.add( file.save() );
        }
        // 更新网关和admin配置
        StatisticsUtils.configureGatewayAndAdminInfo( wsp );
        // 设置统计起始时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        beginDate = calendar.getTime();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor( 180 * 1000 );
        threadDownloadFile downloadFile = new threadDownloadFile();
        threadUploadFile uploadFile = new threadUploadFile();
        es.addWorker( downloadFile );
        es.addWorker( uploadFile );
        es.run();
        Assert.assertEquals( downloadFile.getRetCode(), 0 );
        Assert.assertEquals( uploadFile.getRetCode(), 0 );

        // 设置查询截止时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();
        StatisticsUtils.waitStatisticalInfoCount( fileNums );
        // 取最大响应时间和最小响应时间
        long upMaxTime = Collections.max( uploadTime );
        long upMinTime = Collections.min( uploadTime );
        long downMaxTime = Collections.max( downloadTime );
        long downMinTime = Collections.min( downloadTime );
        // 查询下载接口统计信息
        ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate ).download().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo( null,
                null, null, null, null, null, fileNums / 2, 0, 0, downMaxTime,
                downMinTime, 0 );
        StatisticsUtils.checkScmFileStatisticNewAddInfo( downloadInfo,
                expDownloadInfo );
        // 查询upload接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate ).upload().get();
        // 检查结果
        ScmFileStatisticInfo expUPloadInfo = new ScmFileStatisticInfo( null,
                null, null, null, null, null, fileNums / 2, 0, 0, upMaxTime,
                upMinTime, 0 );
        StatisticsUtils.checkScmFileStatisticNewAddInfo( uploadInfo,
                expUPloadInfo );
        runSuccess = true;
    }

    @AfterClass()
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( int i = 0; i < fileNums; i++ ) {
                    ScmFactory.File.deleteInstance( siteWorkspace,
                            fileIdList.get( i ), true );
                }
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
            } finally {
                ConfUtil.deleteGateWayStatisticalConf();
                if ( siteSession != null ) {
                    siteSession.close();
                }
            }
        }
    }

    private class threadUploadFile extends ResultStore {
        @ExecuteOrder(step = 1)
        private void UploadFile() throws Exception {
            for ( int i = fileNums / 2; i < fileNums; i++ ) {
                int totaluploadTime = ( int ) StatisticsUtils.uploadFile(
                        filePathList.get( i ), fileName, fileIdList,
                        siteWorkspace );
                uploadTime.add( totaluploadTime );
            }
        }
    }

    private class threadDownloadFile extends ResultStore {
        @ExecuteOrder(step = 1)
        private void DownloadFile() throws Exception {
            for ( int i = 0; i < fileNums / 2; i++ ) {
                int totalDownloadTime = ( int ) StatisticsUtils
                        .downloadFile( fileIdList.get( i ), siteWorkspace );
                downloadTime.add( totalDownloadTime );
            }
        }
    }
}