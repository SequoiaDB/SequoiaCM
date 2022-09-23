package com.sequoiacm.testcommon.scmutils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;

import com.sequoiacm.client.common.ScmType.StatisticsType;
import com.sequoiacm.client.common.TrafficType;
import com.sequoiacm.client.exception.ScmException;

/**
 * @Description tatisticsUtils.java
 * @author wuyan
 * @date 2018.9.13
 */
public class StatisticsUtils extends TestScmBase {
    public static final String STATISTICAL_CS = "SCMSYSTEM";
    public static final String STATISTICAL_CL = "STATISTICS_DATA";
    public static final String STATISTICAL_BREAKPOINT_CL = "STATISTICS_BREAKPOINT_FILE";
    public final static int TIMEOUT = 1000 * 120;
    public final static int INTERVAL = 200;

    // get the timestamp of the Day,eg "2018-09-12" is 1536681600000
    public static long getTimestampOfTheDay() {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd" );
        String nowdate = df.format( date );
        long currentTimestamp = 0;
        try {
            currentTimestamp = df.parse( nowdate ).getTime();
        } catch ( ParseException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return currentTimestamp;
    }

    public static HashMap< String, Long > statisticsFile( ScmWorkspace ws,
            ScmSession session ) throws ScmException {
        long currentTimestamp = getTimestampOfTheDay();
        ScmSystem.Statistics.refresh( session, StatisticsType.TRAFFIC,
                ws.getName() );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.Traffic.WORKSPACE_NAME )
                .is( ws.getName() ).put( ScmAttributeName.Traffic.RECORD_TIME )
                .is( currentTimestamp ).get();

        ScmCursor< ScmStatisticsTraffic > cursor = ScmSystem.Statistics
                .listTraffic( session, cond );
        HashMap< String, Long > map = new HashMap< String, Long >();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmStatisticsTraffic trafficInfo = cursor.getNext();
            TrafficType type = trafficInfo.getType();
            long traffic = trafficInfo.getTraffic();
            if ( type.getName().equals( "file_upload" ) ) {
                map.put( "file_upload", traffic );
            } else if ( type.getName().equals( "file_download" ) ) {
                map.put( "file_download", traffic );
            }
            map.put( type.toString(), traffic );
            size++;
        }
        cursor.close();

        // no statistics ,the traffic is 0
        if ( size == 0 ) {
            long traffic = 0;
            map.put( "file_upload", traffic );
            map.put( "file_download", traffic );
        }
        return map;
    }

    public static HashMap< String, Long > statisticsFileDelta( ScmWorkspace ws,
            ScmSession session ) throws ScmException {
        long currentTimestamp = getTimestampOfTheDay();
        ScmSystem.Statistics.refresh( session, StatisticsType.FILE_DELTA,
                ws.getName() );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.Traffic.WORKSPACE_NAME )
                .is( ws.getName() )
                .put( ScmAttributeName.FileDelta.RECORD_TIME )
                .is( currentTimestamp ).get();

        ScmCursor< ScmStatisticsFileDelta > cursor = ScmSystem.Statistics
                .listFileDelta( session, cond );
        HashMap< String, Long > map = new HashMap< String, Long >();
        long count_delta = 0;
        long size_delta = 0;
        while ( cursor.hasNext() ) {
            ScmStatisticsFileDelta traffic = cursor.getNext();
            count_delta = traffic.getCountDelta();
            size_delta = traffic.getSizeDelta();
        }
        cursor.close();

        map.put( "count_delta", count_delta );
        map.put( "size_delta", size_delta );
        return map;
    }

    /**
     * create File by stream
     *
     * @param ws
     * @param fileName
     * @param data
     * @param authorName
     * @throws ScmException
     */
    public static ScmId createFileByStream( ScmWorkspace ws, String fileName,
            byte[] data, String authorName ) throws ScmException {
        return createFileByStream( ws, fileName, data, authorName, 0 );
    }

    public static ScmId createFileByStream( ScmWorkspace ws, String fileName,
            byte[] data, String authorName, long timestamp )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        new Random().nextBytes( data );
        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        if ( timestamp != 0 ) {
            Date date = new Date( timestamp );
            file.setCreateTime( date );
        }

        ScmId fileId = file.save();
        return fileId;
    }

    /**
     * 获取工作区统计信息记录数
     *
     * @param wsName
     * @return
     * @throws Exception
     */
    public static long countWsStatisticalInfo( String wsName )
            throws Exception {
        SiteWrapper site = ScmInfo.getRootSite();
        BSONObject condition = new BasicBSONObject( "workspace", wsName );
        return TestSdbTools.count( site.getMetaDsUrl(), site.getMetaUser(),
                site.getMetaPasswd(), STATISTICAL_CS, STATISTICAL_CL,
                condition );
    }

    /**
     * 清理统计信息表
     *
     * @throws Exception
     */
    public static void clearStatisticalInfo() throws Exception {
        SiteWrapper site = ScmInfo.getRootSite();
        TestSdbTools.delete( site.getMetaDsUrl(), site.getMetaUser(),
                site.getMetaPasswd(), STATISTICAL_CS, STATISTICAL_CL,
                new BasicBSONObject() );
        TestSdbTools.delete( site.getMetaDsUrl(), site.getMetaUser(),
                site.getMetaPasswd(), STATISTICAL_CS, STATISTICAL_BREAKPOINT_CL,
                new BasicBSONObject() );
    }

    /**
     * 限时等待统计记录达到预期值
     *
     * @param count
     * @throws Exception
     */
    public static void waitStatisticalInfoCount( long count ) throws Exception {
        waitStatisticalInfoCount( count, TIMEOUT, INTERVAL );
    }

    /**
     * 限时等待统计记录达到预期值
     *
     * @param count
     * @param timeout
     * @param interval
     * @throws Exception
     */
    public static void waitStatisticalInfoCount( long count, int timeout,
            int interval ) throws Exception {
        int tryNum = timeout / interval;
        long actCount = 0;
        List< BSONObject > info = null;
        while ( tryNum-- > 0 ) {
            SiteWrapper site = ScmInfo.getRootSite();
            info = TestSdbTools.query( site.getMetaDsUrl(), site.getMetaUser(),
                    site.getMetaPasswd(), STATISTICAL_CS, STATISTICAL_CL,
                    new BasicBSONObject() );
            for ( BSONObject bson : info ) {
                BasicBSONObject basicBSONObject = ( BasicBSONObject ) bson;
                actCount += basicBSONObject.getLong( "request_count" );
            }
            if ( actCount == count ) {
                return;
            }
            Thread.sleep( interval );
            if ( tryNum >= 1 ) {
                actCount = 0;
            }
        }
        throw new Exception( "time out,actCount = " + actCount + ",expCount = "
                + count + ",\n info = " + info.toString() );
    }

    /**
     * 设置网关本地时间
     *
     * @param time
     * @throws Exception
     */
    public static void setGateWaySystemTime( long time ) throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( ScmInfo.getSite() );
            List< ScmServiceInstance > instanceList = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session,
                            ConfUtil.GATEWAY_SERVICE_NAME );
            for ( ScmServiceInstance instance : instanceList ) {
                TestTools.setSystemTime( instance.getIp(), time );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    /**
     * 恢复网关本地时间
     *
     * @throws Exception
     */
    public static void restoreGateWaySystemTime() throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( ScmInfo.getSite() );
            List< ScmServiceInstance > instanceList = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session,
                            ConfUtil.GATEWAY_SERVICE_NAME );
            for ( ScmServiceInstance instance : instanceList ) {
                TestTools.restoreSystemTime( instance.getIp() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    /**
     * 检查统计信息
     *
     * @param actInfo
     * @param expInfo
     * @throws Exception
     */
    public static void checkScmFileStatisticInfo( ScmFileStatisticInfo actInfo,
            ScmFileStatisticInfo expInfo ) throws Exception {
        try {
            Assert.assertEquals( actInfo.getType(), expInfo.getType() );
            Assert.assertEquals( actInfo.getBeginDate(),
                    expInfo.getBeginDate() );
            Assert.assertEquals( actInfo.getEndDate(), expInfo.getEndDate() );
            Assert.assertEquals( actInfo.getUser(), expInfo.getUser() );
            Assert.assertEquals( actInfo.getWorkspace(),
                    expInfo.getWorkspace() );
            Assert.assertEquals( actInfo.getTimeAccuracy(),
                    expInfo.getTimeAccuracy() );
            Assert.assertEquals( actInfo.getRequestCount(),
                    expInfo.getRequestCount() );
            Assert.assertEquals( Math
                    .abs( expInfo.getAvgTrafficSize()
                            - actInfo.getAvgTrafficSize() ) >= 0
                    && Math.abs( expInfo.getAvgTrafficSize()
                            - actInfo.getAvgTrafficSize() ) <= expInfo
                                    .getRequestCount(),
                    true );
            Assert.assertEquals( actInfo.getAvgResponseTime() >= 0 && actInfo
                    .getAvgResponseTime() <= expInfo.getAvgResponseTime(),
                    true );
        } catch ( AssertionError e ) {
            throw new Exception( "act = " + actInfo.toString() + "\n,exp = "
                    + expInfo.toString(), e );
        }
    }

    /**
     * 检查新增指标的统计信息 (最大响应时间、最小响应时间、失败数)
     *
     * @param actInfo
     * @param expInfo
     * @throws Exception
     */
    public static void checkScmFileStatisticNewAddInfo(
            ScmFileStatisticInfo actInfo, ScmFileStatisticInfo expInfo )
            throws Exception {
        try {

            Assert.assertEquals( actInfo.getMaxResponseTime() >= 0 && actInfo
                    .getMaxResponseTime() <= expInfo.getMaxResponseTime(),
                    true );
            Assert.assertEquals( actInfo.getMinResponseTime() >= 0 && actInfo
                    .getMinResponseTime() <= expInfo.getMinResponseTime(),
                    true );
            Assert.assertEquals( actInfo.getSuccessCount(),
                    expInfo.getSuccessCount() );
            Assert.assertEquals( actInfo.getFailCount(),
                    expInfo.getFailCount() );
        } catch ( AssertionError e ) {
            throw new Exception( "act = " + actInfo.toString() + "\n,exp = "
                    + expInfo.toString(), e );
        }
    }

    /**
     * 配置网关信息,统计文件上传下载信息
     *
     * @param wsp
     * @throws Exception
     */
    public static void configureGatewayAndAdminInfo( WsWrapper wsp )
            throws Exception {
        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_download,file_upload" );
        confMap1.put(
                "scm.statistics.types.file_download.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    /**
     * 配置网关信息
     * 
     * @param wsp
     * @param tpye
     * @throws Exception
     */
    public static void configureGatewayAndAdminInfo( WsWrapper wsp,
            ScmFileStatisticsType tpye ) throws Exception {
        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", tpye.toString().toLowerCase() );
        if ( tpye.equals( ScmFileStatisticsType.FILE_DOWNLOAD.toString()
                .toLowerCase() ) ) {
            confMap1.put(
                    "scm.statistics.types.file_download.conditions.workspaces",
                    wsp.getName() );
        }
        if ( tpye.equals(
                ScmFileStatisticsType.FILE_UPLOAD.toString().toLowerCase() ) ) {
            confMap1.put(
                    "scm.statistics.types.file_upload.conditions.workspaces",
                    wsp.getName() );
        }
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    /**
     * 创建用户和角色
     *
     * @param rolename
     * @param username
     * @param wsp
     * @param site
     * @throws Exception
     */
    public static void createUserAndRole( String rolename, String username,
            WsWrapper wsp, SiteWrapper site ) throws Exception {
        ScmSession sessionA = null;
        try {
            sessionA = TestScmTools.createSession( site,
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
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

    /**
     * 指定fileId下载文件
     *
     * @param fileId
     * @param siteWorkspace
     *
     * @return
     * @throws Exception
     */
    public static long downloadFile( ScmId fileId, ScmWorkspace siteWorkspace )
            throws Exception {
        long downloadBeginTime = System.nanoTime();
        ScmFile file = ScmFactory.File.getInstance( siteWorkspace, fileId );
        // download file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        long nanoTime = TimeUnit.MILLISECONDS.convert(
                System.nanoTime() - downloadBeginTime, TimeUnit.NANOSECONDS );
        return nanoTime;
    }

    /**
     * 指定fileId下载文件失败
     *
     * @param fileId
     * @param siteWorkspace
     * @throws Exception
     */
    public static void downloadFileFialed( ScmId fileId,
            ScmWorkspace siteWorkspace ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( siteWorkspace, fileId );
        file.delete( true );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            file.getContent( outputStream );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw new Exception(
                        "the exception not FILE_NOT_FOUND,the exception: "
                                + e.getMessage() );
            }
        }
    }

    /**
     * 上传文件
     *
     * @param filePath
     * @param fileName
     * @param fileIdList
     * @param siteWorkspace
     * @return
     * @throws Exception
     */
    public static long uploadFile( String filePath, String fileName,
            List< ScmId > fileIdList, ScmWorkspace siteWorkspace )
            throws Exception {
        long uploadBeginTime = System.nanoTime();
        ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
        file.setFileName( fileName + UUID.randomUUID() );
        file.setAuthor( fileName );
        file.setContent( filePath );
        fileIdList.add( file.save() );
        long uploadTime = TimeUnit.MILLISECONDS.convert(
                System.nanoTime() - uploadBeginTime, TimeUnit.NANOSECONDS );
        return uploadTime;
    }

    /**
     * 上传文件失败
     *
     * @param filePath
     * @param fileName
     * @param fileIdList
     * @param siteWorkspace
     * @return
     * @throws Exception
     */
    public static void uploadFileFialed( String filePath, String fileName,
            List< ScmId > fileIdList, ScmWorkspace siteWorkspace )
            throws Exception {
        try {
            ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
            file.setFileName( fileName + UUID.randomUUID() );
            file.setAuthor( fileName );
            file.setContent( filePath );
            file.setClassProperties(
                    new ScmClassProperties( UUID.randomUUID().toString() ) );
            file.save();
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.METADATA_CLASS_NOT_EXIST
                    .getErrorCode() ) {
                throw new Exception(
                        "the exception not METADATA_CLASS_NOT_EXIST,the exception: "
                                + e.getMessage() );
            }
        }
    }

    /**
     * 生成文件
     *
     * @param fileSizes
     * @param filePathList
     * @return
     * @throws Exception
     */
    public static File createFile( int[] fileSizes,
            List< String > filePathList ) throws Exception {
        File localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSizes[ i ] + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSizes[ i ] );
            filePathList.add( filePath );
        }
        return localPath;
    }

    /**
     * 创建断点文件
     *
     * @param BreakpointFileName
     * @param siteWorkspace
     * @return
     * @throws Exception
     */
    public static long createAndUploadBreakpointFile( String breakpointFileName,
            ScmWorkspace siteWorkspace, String filePath ) throws Exception {
        long uploadBeginTime = System.nanoTime();
        ScmChecksumType checksumType = ScmChecksumType.NONE;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( siteWorkspace, breakpointFileName,
                        checksumType );
        FileInputStream fStream = new FileInputStream( filePath );
        breakpointFile.upload( fStream );
        long uploadTime = TimeUnit.MILLISECONDS.convert(
                System.nanoTime() - uploadBeginTime, TimeUnit.NANOSECONDS );
        return uploadTime;
    }

    /**
     * 断点文件转为文件
     *
     * @param breakpointFile
     * @param fileName
     * @param fileIdList
     * @param siteWorkspace
     * @return
     * @throws Exception
     */
    public static long breakpointFileToFile( String breakpointFileName,
            ScmWorkspace siteWorkspace, String fileName,
            List< ScmId > fileIdList ) throws Exception {
        long uploadBeginTime = System.nanoTime();
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( siteWorkspace, breakpointFileName );
        ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setContent( breakpointFile );
        fileIdList.add( file.save() );
        long uploadTime = TimeUnit.MILLISECONDS.convert(
                System.nanoTime() - uploadBeginTime, TimeUnit.NANOSECONDS );
        return uploadTime;
    }
}
