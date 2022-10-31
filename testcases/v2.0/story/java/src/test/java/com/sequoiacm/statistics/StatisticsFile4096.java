package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.SkipException;
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
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @author ZhangYanan
 * @version v1.0
 * @description SCM-4096:指定时间段内有统计信息，用户查询所有工作区上传接口的统计信息
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 */

public class StatisticsFile4096 extends TestScmBase {
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );
    private SiteWrapper site = null;
    private WsWrapper wsp1 = null;
    private WsWrapper wsp2 = null;
    private String fileName = "file4096_";
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private List< Integer > uploadTime = new ArrayList<>();
    private int fileSize = 200 * 1024;
    private int fileNums = 12;
    private Calendar calendar = null;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();
        if ( ScmInfo.getWsNum() < 2 ) {
            throw new SkipException( "need 2 wss!!!!" );
        }
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        List< WsWrapper > wsList = ScmInfo.getAllWorkspaces();
        wsp1 = wsList.get( 0 );
        wsp2 = wsList.get( 1 );

        // 清理环境和更新配置
        prepareEnv();

        // 制造断点文件上传信息
        prepareRawData();
        StatisticsUtils.waitStatisticalInfoCount( fileNums );
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
    public void test( Date beginDate, Date endDate ) throws Exception {
        // 预期统计数据
        long maxTime = Collections.max( uploadTime );
        long minTime = Collections.min( uploadTime );
        long totalTime = 0;
        for ( long time : uploadTime ) {
            totalTime += time;
        }

        // 查询上传接口统计信息
        try ( ScmSession session = TestScmTools.createSession( site )) {
            ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                    .fileStatistician( session ).user( TestScmBase.scmUserName )
                    .beginDate( beginDate ).endDate( endDate )
                    .timeAccuracy( ScmTimeAccuracy.DAY ).upload().get();

            // 检查结果
            ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                    ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                    TestScmBase.scmUserName, null, ScmTimeAccuracy.DAY,
                    fileNums, fileSize, totalTime, maxTime, minTime, 0 );

            StatisticsUtils.checkScmFileStatisticInfo( uploadInfo,
                    expUploadInfo );
            StatisticsUtils.checkScmFileStatisticNewAddInfo( uploadInfo,
                    expUploadInfo );
        }
        runSuccessCount.incrementAndGet();
    }

    @AfterClass()
    private void tearDown() throws Exception {
        try ( ScmSession session = TestScmTools.createSession( site )) {
            ScmWorkspace ws1 = ScmFactory.Workspace
                    .getWorkspace( wsp1.getName(), session );
            ScmWorkspace ws2 = ScmFactory.Workspace
                    .getWorkspace( wsp2.getName(), session );
            if ( runSuccessCount.get() == generateDate().length
                    || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws1, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws2, fileId, true );
                }
            }
        } finally {
            StatisticsUtils.restoreGateWaySystemTime();
            ConfUtil.deleteGateWayStatisticalConf();
        }
    }

    private void prepareRawData() throws Exception {
        // ws1存在多个断点文件上传记录
        int createAndUploadBreakpointFileTime;
        int breakpointFileToFileTime;
        try ( ScmSession session = TestScmTools.createSession( site )) {
            ScmWorkspace ws1 = ScmFactory.Workspace
                    .getWorkspace( wsp1.getName(), session );
            createAndUploadBreakpointFileTime = ( int ) StatisticsUtils
                    .createAndUploadBreakpointFile( fileName + 0, ws1,
                            filePath );
            breakpointFileToFileTime = ( int ) StatisticsUtils
                    .breakpointFileToFile( fileName + 0, ws1, fileName + 0,
                            fileIdList1 );
            uploadTime.add( createAndUploadBreakpointFileTime
                    + breakpointFileToFileTime );
        }

        // 网关时间跳变，制造多条记录
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        StatisticsUtils.setGateWaySystemTime( calendar.getTimeInMillis() );

        try ( ScmSession session = TestScmTools.createSession( site )) {
            ScmWorkspace ws1 = ScmFactory.Workspace
                    .getWorkspace( wsp1.getName(), session );
            for ( int i = 1; i < fileNums / 2; i++ ) {
                createAndUploadBreakpointFileTime = ( int ) StatisticsUtils
                        .createAndUploadBreakpointFile( fileName + i, ws1,
                                filePath );
                breakpointFileToFileTime = ( int ) StatisticsUtils
                        .breakpointFileToFile( fileName + i, ws1, fileName + i,
                                fileIdList1 );
                uploadTime.add( createAndUploadBreakpointFileTime
                        + breakpointFileToFileTime );
            }
        }

        // ws2有断点文件上传记录
        try ( ScmSession session = TestScmTools.createSession( site )) {
            ScmWorkspace ws2 = ScmFactory.Workspace
                    .getWorkspace( wsp2.getName(), session );
            createAndUploadBreakpointFileTime = ( int ) StatisticsUtils
                    .createAndUploadBreakpointFile( fileName + fileNums / 2,
                            ws2, filePath );
            breakpointFileToFileTime = ( int ) StatisticsUtils
                    .breakpointFileToFile( fileName + fileNums / 2, ws2,
                            fileName + fileNums / 2, fileIdList2 );
            uploadTime.add( createAndUploadBreakpointFileTime
                    + breakpointFileToFileTime );
        }

        // 网关时间跳变，制造多条记录
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        StatisticsUtils.setGateWaySystemTime( calendar.getTimeInMillis() );

        try ( ScmSession session = TestScmTools.createSession( site )) {
            ScmWorkspace ws2 = ScmFactory.Workspace
                    .getWorkspace( wsp2.getName(), session );
            for ( int i = fileNums / 2 + 1; i < fileNums; i++ ) {
                createAndUploadBreakpointFileTime = ( int ) StatisticsUtils
                        .createAndUploadBreakpointFile( fileName + i, ws2,
                                filePath );
                breakpointFileToFileTime = ( int ) StatisticsUtils
                        .breakpointFileToFile( fileName + i, ws2, fileName + i,
                                fileIdList2 );
                uploadTime.add( createAndUploadBreakpointFileTime
                        + breakpointFileToFileTime );
            }
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
}