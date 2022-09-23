package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;

import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @description SCM-3819:不同用户下，文件上传部分失败，查询统计信息。
 * @author ZhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile4097 extends TestScmBase {
    private File localPath = null;
    private String username1 = "user4097A";
    private String rolename1 = "role4097A";
    private String username2 = "user4097B";
    private String rolename2 = "role4097B";
    private String filePath = null;
    private int uploadFilesSuccedNums1 = 6;
    private int uploadFilesSuccedNums2 = 4;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String fileName = "file3819";
    private ScmSession siteSession = null;
    private ScmSession siteSession1 = null;
    private ScmSession siteSession2 = null;
    private ScmWorkspace siteWorkspace1 = null;
    private ScmWorkspace siteWorkspace2 = null;
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private int fileSize = 200 * 1024;
    private int fileNums = 10;
    private List< Integer > uploadTime1 = new ArrayList<>();
    private List< Integer > uploadTime2 = new ArrayList<>();
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;

    @BeforeClass
    public void setUp() throws Exception {
        calendar = Calendar.getInstance();
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBDataSource();

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );

        // 创建用户
        StatisticsUtils.createUserAndRole( rolename1, username1, wsp, site );
        StatisticsUtils.createUserAndRole( rolename2, username2, wsp, site );

        siteSession = TestScmTools.createSession( site );
        siteSession1 = TestScmTools.createSession( site, username1, username1 );
        siteSession2 = TestScmTools.createSession( site, username2, username2 );

        siteWorkspace1 = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                siteSession1 );
        siteWorkspace2 = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                siteSession2 );

        // 更新网关和admin配置
        StatisticsUtils.configureGatewayAndAdminInfo( wsp );
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
        // 设置查询截止时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();

        checkScmFileStatInfo( siteSession1, uploadTime1, username1,
                uploadFilesSuccedNums1 );
        checkScmFileStatInfo( siteSession2, uploadTime2, username2,
                uploadFilesSuccedNums2 );
        runSuccess = true;
    }

    @AfterClass()
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( siteWorkspace1, fileId,
                            true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( siteWorkspace2, fileId,
                            true );
                }
                ScmFactory.Role.deleteRole( siteSession, rolename1 );
                ScmFactory.User.deleteUser( siteSession, username1 );
                ScmFactory.Role.deleteRole( siteSession, rolename2 );
                ScmFactory.User.deleteUser( siteSession, username2 );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                ConfUtil.deleteGateWayStatisticalConf();
                if ( siteSession != null ) {
                    siteSession.close();
                }
                if ( siteSession1 != null ) {
                    siteSession1.close();
                }
                if ( siteSession2 != null ) {
                    siteSession2.close();
                }
            }
        }
    }

    public static void checkScmFileStatisticInfo( ScmFileStatisticInfo actInfo,
            ScmFileStatisticInfo expInfo ) throws Exception {
        try {
            Assert.assertEquals( actInfo.getFailCount(),
                    expInfo.getFailCount() );
            Assert.assertEquals( actInfo.getUser(), expInfo.getUser() );
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

    public void constructStatisticsInfo() throws Exception {
        int createAndUploadBreakpointFileTime;
        int breakpointFileToFileTime;
        // 上传断点文件
        for ( int i = 0; i < uploadFilesSuccedNums1; i++ ) {
            createAndUploadBreakpointFileTime = ( int ) StatisticsUtils
                    .createAndUploadBreakpointFile( fileName + i,
                            siteWorkspace1, filePath );
            breakpointFileToFileTime = ( int ) StatisticsUtils
                    .breakpointFileToFile( fileName + i, siteWorkspace1,
                            fileName + i, fileIdList1 );
            uploadTime1.add( createAndUploadBreakpointFileTime
                    + breakpointFileToFileTime );
        }

        // user2上传文件
        for ( int i = uploadFilesSuccedNums1; i < uploadFilesSuccedNums1
                + uploadFilesSuccedNums2; i++ ) {
            createAndUploadBreakpointFileTime = ( int ) StatisticsUtils
                    .createAndUploadBreakpointFile( fileName + i,
                            siteWorkspace2, filePath );
            breakpointFileToFileTime = ( int ) StatisticsUtils
                    .breakpointFileToFile( fileName + i, siteWorkspace2,
                            fileName + i, fileIdList2 );
            uploadTime2.add( createAndUploadBreakpointFileTime
                    + breakpointFileToFileTime );
        }
    }

    public void checkScmFileStatInfo( ScmSession siteSession,
            List< Integer > uploadTime, String scmUserName, int requestCount )
            throws Exception {
        // 取最大响应时间和最小响应时间
        long maxTime = Collections.max( uploadTime );
        long minTime = Collections.min( uploadTime );
        long totalTime = 0;
        for ( long time : uploadTime ) {
            totalTime += time;
        }

        // 查询接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( scmUserName )
                .workspace( wsp.getName() ).beginDate( beginDate )
                .endDate( endDate ).upload().get();
        // 检查结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                scmUserName, wsp.getName(), null, requestCount, fileSize,
                totalTime / requestCount, maxTime, minTime, 0 );

        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo, expUploadInfo );
        StatisticsUtils.checkScmFileStatisticNewAddInfo( uploadInfo,
                expUploadInfo );
    }
}
