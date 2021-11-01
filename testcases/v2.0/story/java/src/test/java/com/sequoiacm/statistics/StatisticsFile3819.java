package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;

import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.apache.directory.shared.kerberos.codec.apRep.actions.ApRepInit;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
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
public class StatisticsFile3819 extends TestScmBase {
    private File localPath = null;
    private String username1 = "user3819A";
    private String rolename1 = "role3819A";
    private String username2 = "user3819B";
    private String rolename2 = "role3819B";
    private int uploadFilesFaidNums1 = 6;
    private int uploadFilesFaidNums2 = 7;
    private int uploadFilesSuccedNums1 = 4;
    private int uploadFilesSuccedNums2 = 3;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private String fileName = "file3819";
    private ScmSession siteSession = null;
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
    private Calendar calendar = Calendar.getInstance();
    private List< String > filePathList = new ArrayList<>();

    @BeforeClass
    public void setUp() throws Exception {
        fileNums = fileSizes.length;
        localPath = StatisticsUtils.createFile( fileSizes, filePathList );
        wsp = ScmInfo.getWs();
        site = ScmInfo.getBranchSite();

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

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        // 更新网关和admin配置
        ScmFileStatisticsType statisticType = ScmFileStatisticsType.FILE_UPLOAD;
        StatisticsUtils.configureGatewayAndAdminInfo( wsp, statisticType );
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
                uploadFilesFaidNums1 );
        checkScmFileStatInfo( siteSession2, uploadTime2, username2,
                uploadFilesFaidNums2 );
        runSuccess = true;
    }

    @AfterClass()
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( int i = 0; i < uploadFilesSuccedNums1
                        + uploadFilesSuccedNums2; i++ ) {
                    ScmFactory.File.deleteInstance( siteWorkspace1,
                            fileIdList.get( i ), true );
                }
                ScmFactory.Role.deleteRole( siteSession, rolename1 );
                ScmFactory.User.deleteUser( siteSession, username1 );
                ScmFactory.Role.deleteRole( siteSession, rolename2 );
                ScmFactory.User.deleteUser( siteSession, username2 );
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
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
        // user1上传文件
        for ( int i = 0; i < uploadFilesSuccedNums1; i++ ) {
            int totalUploadTime = ( int ) StatisticsUtils.uploadFile(
                    filePathList.get( i ), fileName, fileIdList,
                    siteWorkspace1 );
            uploadTime1.add( totalUploadTime );
        }
        int count = uploadFilesSuccedNums1 + uploadFilesFaidNums1;
        for ( int i = uploadFilesSuccedNums1; i < count; i++ ) {
            StatisticsUtils.uploadFileFialed( filePathList.get( i ), fileName,
                    fileIdList, siteWorkspace1 );
        }
        // user2上传文件
        for ( int i = count; i < count + uploadFilesSuccedNums2; i++ ) {
            int totalUploadTime = ( int ) StatisticsUtils.uploadFile(
                    filePathList.get( i ), fileName, fileIdList,
                    siteWorkspace2 );
            uploadTime2.add( totalUploadTime );
        }
        for ( int i = count + uploadFilesSuccedNums2; i < fileNums; i++ ) {
            StatisticsUtils.uploadFileFialed( filePathList.get( i ), fileName,
                    fileIdList, siteWorkspace2 );
        }
    }

    public void checkScmFileStatInfo( ScmSession siteSession,
            List< Integer > uploadTime, String scmUserName, int failcount )
            throws Exception {
        // 取最大响应时间和最小响应时间
        long maxTime = Collections.max( uploadTime );
        long minTime = Collections.min( uploadTime );

        // 查询接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( scmUserName )
                .workspace( wsp.getName() ).beginDate( beginDate )
                .endDate( endDate ).upload().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo( null,
                null, null, scmUserName, null, null, fileNums / 2, 0, 0,
                maxTime, minTime, failcount );
        checkScmFileStatisticInfo( uploadInfo, expDownloadInfo );
    }
}
