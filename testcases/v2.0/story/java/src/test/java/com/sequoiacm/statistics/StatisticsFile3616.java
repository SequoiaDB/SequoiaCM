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
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @Description: SCM-3616:统计表中有多个用户统计信息，查询所有用户上传/下载接口统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3616 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String username = "user3616";
    private String rolename = "role3616";
    private String fileNameBase = "file3616";
    private List< ScmId > fileIdList = new ArrayList<>();
    private int[] fileSizes = { 200 * 1024, 2, 1024, 2, 700 * 1024, 300 * 1024,
            6, 100 * 1024, 10 };
    private int totalFileSize = 0;
    private int fileNums = fileSizes.length;
    private int totalUploadTime = 0;
    private int totalDownloadTime = 0;
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;
    private File localPath = null;
    private List< String > filePathList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSizes[ i ] + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSizes[ i ] );
            totalFileSize += fileSizes[ i ];
            filePathList.add( filePath );
        }
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        // 清理环境和更新配置
        prepareEnv();
        // 创建用户和角色
        createUserAndRole();
        // 制造上传和下载请求信息
        prepareRawData();
        StatisticsUtils.waitStatisticalInfoCount( fileNums * 4 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - fileNums * 5 );
        beginDate = calendar.getTime();

        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            // 查询上传接口统计信息
            ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                    .fileStatistician( session ).beginDate( beginDate )
                    .endDate( endDate ).workspace( wsp.getName() )
                    .timeAccuracy( ScmTimeAccuracy.DAY ).upload().get();
            // 检查结果
            ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                    ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate, null,
                    wsp.getName(), ScmTimeAccuracy.DAY, fileNums * 2,
                    totalFileSize / fileNums, totalUploadTime / fileNums * 2 );
            StatisticsUtils.checkScmFileStatisticInfo( uploadInfo,
                    expUploadInfo );

            // 查询下载接口统计信息
            ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                    .fileStatistician( session ).workspace( wsp.getName() )
                    .beginDate( beginDate ).endDate( endDate )
                    .timeAccuracy( ScmTimeAccuracy.DAY ).download().get();
            // 检查结果
            ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                    ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                    null, wsp.getName(), ScmTimeAccuracy.DAY, fileNums * 2,
                    totalFileSize / fileNums,
                    totalDownloadTime / ( fileNums * 2 ) );
            StatisticsUtils.checkScmFileStatisticInfo( downloadInfo,
                    expDownloadInfo );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.Role.deleteRole( session, rolename );
                ScmFactory.User.deleteUser( session, username );
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

    private void prepareRawData() throws Exception {
        // 上传文件
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        totalUploadTime += createFiles( TestScmBase.scmUserName,
                TestScmBase.scmPassword, calendar.getTimeInMillis() );

        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        totalUploadTime += createFiles( username, username,
                calendar.getTimeInMillis() );

        // 下载文件
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        totalDownloadTime += downloadFiles( TestScmBase.scmUserName,
                TestScmBase.scmPassword, fileIdList.subList( 0, fileNums ),
                calendar.getTimeInMillis() );

        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        totalDownloadTime += downloadFiles( username, username,
                fileIdList.subList( fileNums, fileNums * 2 ),
                calendar.getTimeInMillis() );
    }

    private void prepareEnv() throws Exception {
        // 清理环境
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        ConfUtil.deleteGateWayStatisticalConf();

        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download,file_upload" );
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

    private long createFiles( String username, String password,
            long gateWayLocalTime ) throws Exception {
        StatisticsUtils.setGateWaySystemTime( gateWayLocalTime );
        long uploadBeginTime = System.currentTimeMillis();
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            for ( int i = 0; i < filePathList.size(); i++ ) {
                // 时间条变可能使长连接失效，所以这里重试创建文件
                for ( int j = 0; j < 3; j++ ) {
                    try {
                        createFile( ws, filePathList.get( i ) );
                        break;
                    } catch ( ScmException e ) {
                        Thread.sleep( 200 );
                        continue;
                    }
                }
            }
            return System.currentTimeMillis() - uploadBeginTime;
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private long downloadFiles( String username, String password,
            List< ScmId > fileIdList, long gateWayLocalTime ) throws Exception {
        StatisticsUtils.setGateWaySystemTime( gateWayLocalTime );
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            long downloadBeginTime = System.currentTimeMillis();
            for ( ScmId fileId : fileIdList ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                // download file
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                file.getContent( outputStream );
            }
            return System.currentTimeMillis() - downloadBeginTime;
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws, String filePath )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
        file.setAuthor( fileNameBase );
        file.setContent( filePath );
        fileIdList.add( file.save() );
    }

    private void createUserAndRole() throws Exception {
        ScmSession sessionA = null;
        try {
            sessionA = TestScmTools.createSession( site );
            // 清理环境
            try {
                ScmFactory.Role.deleteRole( sessionA, rolename );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    throw e;
                }
            }
            try {
                ScmFactory.User.deleteUser( sessionA, username );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    throw e;
                }
            }
            // 创建用户、角色和授权
            ScmUser scmUser = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, username );
            ScmRole role = ScmFactory.Role.createRole( sessionA, rolename, "" );
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.addRole( role );
            ScmFactory.User.alterUser( sessionA, scmUser, modifier );
            ScmResource resource = ScmResourceFactory
                    .createWorkspaceResource( wsp.getName() );
            ScmFactory.Role.grantPrivilege( sessionA, role, resource,
                    ScmPrivilegeType.ALL );
            ScmAuthUtils.checkPriority( site, username, username, role,
                    wsp.getName() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }
}