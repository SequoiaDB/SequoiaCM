package com.sequoiacm.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

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
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @description SCM-4090:多个断点文件上传，部分失败，查看统计信息
 * @author ZhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile4090 extends TestScmBase {
    private File localPath = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String fileName = "file4090_";
    private ScmSession siteSession = null;
    private ScmWorkspace siteWorkspace = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private List< Integer > uploadTime = new ArrayList<>();
    private int fileNums;
    private List< String > filePathList = new ArrayList<>();
    private int[] fileSizes = { 200001 * 1024, 10000, 1024, 20000, 700 * 1024,
            300 * 1024, 5, 100 * 1024, 1024 * 1024 * 11, 800 * 1024, 900 * 1024,
            10 * 1024, 50 * 1024, 90 * 1024, 60 * 1024, 2024 * 2024, 6 * 1024,
            700 * 1024, 99, 6 };
    private int uploadBreakpointFileFailedNums = 5;
    private int breakpointFileToFileFailedNums = 7;
    private int uploadBreakpointFileSuccess;
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;

    @BeforeClass
    public void setUp() throws Exception {
        calendar = Calendar.getInstance();
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBDataSource();
        fileNums = fileSizes.length;
        uploadBreakpointFileSuccess = fileSizes.length
                - uploadBreakpointFileFailedNums
                - breakpointFileToFileFailedNums;
        localPath = StatisticsUtils.createFile( fileSizes, filePathList );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        siteSession = TestScmTools.createSession( site );
        siteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                siteSession );
        ConfUtil.deleteGateWayStatisticalConf();

        // 提前创建文件和断点文件，构造断点文件上传失败和断点文件转为文件失败
        for ( int i = 0; i < uploadBreakpointFileFailedNums; i++ ) {
            StatisticsUtils.createAndUploadBreakpointFile( fileName + i,
                    siteWorkspace, filePathList.get( i ) );
        }

        for ( int i = uploadBreakpointFileFailedNums; i < uploadBreakpointFileFailedNums
                + breakpointFileToFileFailedNums; i++ ) {
            fileIdList.add( ScmFileUtils.create( siteWorkspace, fileName + i,
                    filePathList.get( i ) ) );
        }

        // 更新网关和admin配置
        ConfUtil.deleteGateWayStatisticalConf();
        StatisticsUtils.configureGatewayAndAdminInfo( wsp );
        // 设置统计起始时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        beginDate = calendar.getTime();
        prepareStatisticsInfo();
        StatisticsUtils.waitStatisticalInfoCount(
                uploadBreakpointFileSuccess + breakpointFileToFileFailedNums );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        // 设定统计结束时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();

        // 预期统计数据
        long maxTime = Collections.max( uploadTime );
        long minTime = Collections.min( uploadTime );
        long totalTime = 0;
        for ( long time : uploadTime ) {
            totalTime += time;
        }
        int expAvgTrafficSize = 0;
        for ( int i = uploadBreakpointFileFailedNums
                + breakpointFileToFileFailedNums; i < fileNums; i++ ) {
            expAvgTrafficSize += fileSizes[ i ];
        }
        expAvgTrafficSize = expAvgTrafficSize / uploadBreakpointFileSuccess;

        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).workspace( wsp.getName() )
                .endDate( endDate ).upload().get();

        // 预期结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), null,
                uploadBreakpointFileSuccess + breakpointFileToFileFailedNums,
                expAvgTrafficSize, totalTime / uploadBreakpointFileSuccess,
                maxTime, minTime, breakpointFileToFileFailedNums );
        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo, expUploadInfo );
        StatisticsUtils.checkScmFileStatisticNewAddInfo( uploadInfo,
                expUploadInfo );
        runSuccess = true;
    }

    @AfterClass()
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId scmId : fileIdList ) {
                    ScmFactory.File.deleteInstance( siteWorkspace, scmId,
                            true );
                }
                for ( int i = 0; i < breakpointFileToFileFailedNums
                        + uploadBreakpointFileFailedNums; i++ ) {
                    ScmFactory.BreakpointFile.deleteInstance( siteWorkspace,
                            fileName + i );
                }
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                ConfUtil.deleteGateWayStatisticalConf();
                if ( siteSession != null ) {
                    siteSession.close();
                }
            }
        }
    }

    public void prepareStatisticsInfo() throws Exception {
        for ( int i = 0; i < uploadBreakpointFileFailedNums; i++ ) {
            try {
                // 断点文件续传失败
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .createInstance( siteWorkspace, fileName + i );
                FileInputStream fStream = new FileInputStream(
                        filePathList.get( i ) );
                breakpointFile.upload( fStream );
                Assert.fail(
                        "uploadBreakPointFile should failed but success!" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_EXIST.getErrorCode() ) {
                    throw e;
                }
            }
        }

        for ( int i = uploadBreakpointFileFailedNums; i < uploadBreakpointFileFailedNums
                + breakpointFileToFileFailedNums; i++ ) {
            try {
                StatisticsUtils.createAndUploadBreakpointFile( fileName + i,
                        siteWorkspace, filePathList.get( i ) );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( siteWorkspace, fileName + i );
                ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
                file.setFileName( fileName + i );
                file.setAuthor( fileName + i );
                file.setContent( breakpointFile );
                file.save();
                Assert.fail(
                        "BreakPointFileToFile should failed but success!" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_EXIST.getErrorCode() ) {
                    throw e;
                }
            }
        }

        int createAndUploadBreakpointFileTime;
        int breakpointFileToFileTime;
        for ( int i = uploadBreakpointFileFailedNums
                + breakpointFileToFileFailedNums; i < fileNums; i++ ) {
            createAndUploadBreakpointFileTime = ( int ) StatisticsUtils
                    .createAndUploadBreakpointFile( fileName + i, siteWorkspace,
                            filePathList.get( i ) );
            breakpointFileToFileTime = ( int ) StatisticsUtils
                    .breakpointFileToFile( fileName + i, siteWorkspace,
                            fileName + i, fileIdList );
            uploadTime.add( createAndUploadBreakpointFileTime
                    + breakpointFileToFileTime );
        }
    }
}