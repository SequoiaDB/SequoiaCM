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
 * @Description: SCM-3592:统计表中有多个用户有统计信息，单个用户查询上传/下载接口统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3592 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String username = "user3592";
    private String rolename = "role3592";
    private String fileNameBase = "file3592";
    private List< ScmId > fileIdList = new ArrayList<>();
    private int[] fileSizes = { 201 * 1024, 1, 1024, 0, 700 * 1024, 300 * 1024,
            5, 100 * 1024, 1024 * 1024 * 11 };
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
        calendar = Calendar.getInstance();
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        // 创建用户和角色
        createUserAndRole();
        // 清理环境和更新配置
        prepareEnv();
        // 制造上传和下载请求信息
        prepareRawData();
        StatisticsUtils.waitStatisticalInfoCount( fileNums * 2 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - fileNums * 5 );
        beginDate = calendar.getTime();

        // 查询上传接口统计信息
        ScmSession sessionB = null;
        try {
            sessionB = TestScmTools.createSession( site, username, username );
            ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                    .fileStatistician( sessionB ).user( username )
                    .beginDate( beginDate ).endDate( endDate ).upload().get();
            // 检查结果
            ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                    ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                    username, null, null, fileNums, totalFileSize / fileNums,
                    totalUploadTime / fileNums );
            StatisticsUtils.checkScmFileStatisticInfo( uploadInfo,
                    expUploadInfo );
        } finally {
            if ( sessionB != null ) {
                sessionB.close();
            }
        }

        // 查询下载接口统计信息
        ScmSession sessionA = null;
        try {
            sessionA = TestScmTools.createSession( site, username, username );
            ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                    .fileStatistician( sessionA ).user( username )
                    .beginDate( beginDate ).endDate( endDate ).download().get();
            // 检查结果
            ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                    ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                    username, null, null, fileNums, totalFileSize / fileNums,
                    totalDownloadTime / fileNums );
            StatisticsUtils.checkScmFileStatisticInfo( downloadInfo,
                    expDownloadInfo );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmSession sessionA = null;
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                sessionA = TestScmTools.createSession( site );
                ScmWorkspace wsA = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionA );
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.Role.deleteRole( sessionA, rolename );
                ScmFactory.User.deleteUser( sessionA, username );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsA, fileId, true );
                }
            }
        } finally {
            StatisticsUtils.restoreGateWaySystemTime();
            ConfUtil.deleteGateWayStatisticalConf();
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void prepareRawData() throws Exception {
        // 有多条上传信息
        for ( int i = 0; i < filePathList.size(); i++ ) {
            totalUploadTime += createFiles( TestScmBase.scmUserName,
                    TestScmBase.scmPassword, filePathList.get( i ),
                    calendar.getTimeInMillis() );
            calendar.set( Calendar.DAY_OF_YEAR,
                    calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        }

        for ( int i = 0; i < filePathList.size(); i++ ) {
            totalUploadTime += createFiles( username, username,
                    filePathList.get( i ), calendar.getTimeInMillis() );
            calendar.set( Calendar.DAY_OF_YEAR,
                    calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        }

        // 有多条下载信息
        for ( ScmId fileId : fileIdList.subList( 0, fileNums ) ) {
            totalDownloadTime += downloadFile( TestScmBase.scmUserName,
                    TestScmBase.scmPassword, fileId,
                    calendar.getTimeInMillis() );
            calendar.set( Calendar.DAY_OF_YEAR,
                    calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        }

        for ( ScmId fileId : fileIdList.subList( fileNums, fileNums * 2 ) ) {
            totalDownloadTime += downloadFile( username, username, fileId,
                    calendar.getTimeInMillis() );
            calendar.set( Calendar.DAY_OF_YEAR,
                    calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        }
    }

    private void prepareEnv() throws Exception {
        // 清理环境
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download,file_upload" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "20" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1000" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    private long createFiles( String username, String password, String filePath,
            long gateWayLocalTime ) throws Exception {
        StatisticsUtils.setGateWaySystemTime( gateWayLocalTime );
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            long uploadBeginTime = System.currentTimeMillis();
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
            file.setAuthor( fileNameBase );
            file.setContent( filePath );
            fileIdList.add( file.save() );
            return System.currentTimeMillis() - uploadBeginTime;
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private long downloadFile( String username, String password, ScmId fileId,
            long gateWayLocalTime ) throws Exception {
        StatisticsUtils.setGateWaySystemTime( gateWayLocalTime );
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            long downloadBeginTime = System.currentTimeMillis();
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            // download file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            file.getContent( outputStream );
            return System.currentTimeMillis() - downloadBeginTime;
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
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