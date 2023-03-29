package com.sequoiacm.testcommon.scmutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import org.apache.log4j.Logger;
import org.bson.BSONObject;

import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import org.bson.BasicBSONObject;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScmFileUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( ScmFileUtils.class );
    private static AtomicInteger serial = new AtomicInteger(
            ( new Random() ).nextInt() );

    /**
     * @descreption 指定文件内容创建文件
     * @param ws
     * @param fileName
     * @param filePath
     * @return ScmId
     * @throws Exception
     */
    public static ScmId create( ScmWorkspace ws, String fileName,
            String filePath ) throws ScmException {
        return create( ws, fileName, filePath, fileName );
    }

    /**
     * @descreption 清理工作区下文件
     * @param ws
     * @param condition
     * @return
     * @throws Exception
     */
    public static void cleanFile( WsWrapper ws, BSONObject condition )
            throws ScmException {
        cleanFile( ws.getName(), condition );
    }

    /**
     * @descreption 清理工作区下文件
     * @param wsName
     * @param condition
     * @return
     * @throws Exception
     */
    public static void cleanFile( String wsName, BSONObject condition )
            throws ScmException {
        ScmSession session = null;
        SiteWrapper site = ScmInfo.getSite();
        ScmCursor< ScmFileBasicInfo > cursor = null;
        ScmId fileId = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmWorkspace work = ScmFactory.Workspace.getWorkspace( wsName,
                    session );

            cursor = ScmFactory.File.listInstance( work,
                    ScopeType.SCOPE_CURRENT, condition );
            while ( cursor.hasNext() ) {
                ScmFileBasicInfo fileInfo = cursor.getNext();
                fileId = fileInfo.getFileId();
                ScmFactory.File.deleteInstance( work, fileId, true );
            }
        } catch ( ScmException e ) {
            logger.error( "[test] clean scmfile, siteName = "
                    + site.getSiteName() + ", fileId=" + fileId );
            e.printStackTrace();
            throw e;
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    /**
     * @descreption 指定文件名清理工作区下文件
     * @param work
     * @param fileName
     * @return
     * @throws Exception
     */
    public static void cleanFile( ScmWorkspace work, String fileName )
            throws ScmException {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            BSONObject matchByName = new BasicBSONObject();
            matchByName.put( ScmAttributeName.File.FILE_NAME, fileName );
            cursor = ScmFactory.File.listInstance( work,
                    ScmType.ScopeType.SCOPE_CURRENT, matchByName );
            while ( cursor.hasNext() ) {
                ScmFileBasicInfo fileInfo = cursor.getNext();
                ScmId fileId = fileInfo.getFileId();
                ScmFactory.File.deleteInstance( work, fileId, true );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            throw e;
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    /**
     * @descreption 校验单个文件元数据和数据存在的站点
     * @param ws
     * @param fileId
     * @param expSites
     * @param localPath
     * @param filePath
     * @return
     * @throws Exception
     */
    public static void checkMetaAndData( WsWrapper ws, ScmId fileId,
            SiteWrapper[] expSites, java.io.File localPath, String filePath )
            throws Exception {
        checkMetaAndData( ws.getName(), fileId, expSites, localPath, filePath );
    }

    /**
     * @descreption 校验单个文件元数据和数据存在的站点
     * @param wsName
     * @param fileId
     * @param expSites
     * @param localPath
     * @param filePath
     * @return
     * @throws Exception
     */
    public static void checkMetaAndData( String wsName, ScmId fileId,
            SiteWrapper[] expSites, java.io.File localPath, String filePath )
            throws Exception {
        List< ScmId > fileIdList = new ArrayList<>();
        fileIdList.add( fileId );
        checkMetaAndData( wsName, fileIdList, expSites, localPath, filePath );
    }

    /**
     * @descreption 校验多个文件的元数据和数据存在的站点
     * @param ws
     * @param fileIdList
     * @param expSites
     * @param localPath
     * @param filePath
     * @return
     * @throws Exception
     */
    public static void checkMetaAndData( WsWrapper ws, List< ScmId > fileIdList,
            SiteWrapper[] expSites, java.io.File localPath, String filePath )
            throws Exception {
        checkMetaAndData( ws.getName(), fileIdList, expSites, localPath,
                filePath );
    }

    /**
     * @descreption 校验多个文件的元数据和数据存在的站点
     * @param wsName
     * @param fileIdList
     * @param expSites
     * @param localPath
     * @param filePath
     * @return
     * @throws Exception
     */
    public static void checkMetaAndData( String wsName,
            List< ScmId > fileIdList, SiteWrapper[] expSites,
            java.io.File localPath, String filePath ) throws Exception {
        boolean medaChecked = false;
        for ( SiteWrapper site : expSites ) {
            ScmSession session = null;
            ScmWorkspace work = null;
            ScmId fileId = null;
            try {
                session = ScmSessionUtils.createSession( site );
                work = ScmFactory.Workspace.getWorkspace( wsName, session );

                for ( int i = 0; i < fileIdList.size(); i++ ) {
                    fileId = fileIdList.get( i );
                    if ( !medaChecked ) {
                        checkMeta( work, fileId, expSites );
                    }
                    checkData( work, fileId, localPath, filePath );
                }

                medaChecked = true;
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    /**
     * @descreption 校验历史版本文件的元数据和数据
     * @param wsName
     * @param fileIdList
     * @param expSites
     * @param localPath
     * @param filePath
     * @param majorVersion
     * @param minorVersion
     * @return
     * @throws Exception
     */
    public static void checkHistoryFileMetaAndData( String wsName,
            List< ScmId > fileIdList, SiteWrapper[] expSites,
            java.io.File localPath, String filePath, int majorVersion,
            int minorVersion ) throws Exception {
        boolean medaChecked = false;
        for ( SiteWrapper site : expSites ) {
            ScmSession session = null;
            ScmWorkspace work = null;
            ScmId fileId = null;
            try {
                session = ScmSessionUtils.createSession( site );
                work = ScmFactory.Workspace.getWorkspace( wsName, session );

                for ( int i = 0; i < fileIdList.size(); i++ ) {
                    fileId = fileIdList.get( i );
                    if ( !medaChecked ) {
                        checkHistoryMeta( work, fileId, expSites, majorVersion,
                                minorVersion );
                    }
                    checkHistoryData( work, fileId, localPath, filePath,
                            majorVersion, minorVersion );
                }

                medaChecked = true;
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    /**
     * @descreption 校验文件当前版本元数据
     * @param ws
     * @param fileId
     * @param expSites
     * @return
     * @throws Exception
     */
    public static void checkMeta( ScmWorkspace ws, ScmId fileId,
            SiteWrapper[] expSites ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        checkMeta( ws, file, expSites );
    }

    /**
     * @descreption 校验文件历史版本元数据
     * @param ws
     * @param fileId
     * @param expSites
     * @return
     * @throws Exception
     */
    public static void checkHistoryMeta( ScmWorkspace ws, ScmId fileId,
            SiteWrapper[] expSites, int majorVersion, int minorVersion )
            throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, majorVersion,
                minorVersion );
        checkMeta( ws, file, expSites );
    }

    /**
     * @descreption 校验文件元数据
     * @param ws
     * @param file
     * @param expSites
     * @return
     * @throws Exception
     */
    public static void checkMeta( ScmWorkspace ws, ScmFile file,
            SiteWrapper[] expSites ) throws Exception {
        ScmId fileId = file.getFileId();
        // sort the actual siteId
        int actSiteNum = file.getLocationList().size();
        List< Integer > actIdList = new ArrayList<>();
        for ( int i = 0; i < actSiteNum; i++ ) {
            int siteId = file.getLocationList().get( i ).getSiteId();
            actIdList.add( siteId );
        }
        Collections.sort( actIdList );

        // sort the expect siteId
        List< Integer > expIdList = new ArrayList<>();
        for ( int i = 0; i < expSites.length; i++ ) {
            expIdList.add( expSites[ i ].getSiteId() );
        }
        Collections.sort( expIdList );

        // check site number
        int expSiteNum = expSites.length;
        if ( actSiteNum != expSiteNum ) {
            throw new Exception( "Failed to check siteNum, ws = " + ws.getName()
                    + ", fileId = " + fileId.get() + ", expSiteNum = "
                    + expSiteNum + ", actSiteNum = " + actSiteNum
                    + ", expSiteIds = " + expIdList + ", actSiteIds = "
                    + actIdList );
        }

        // check site id
        for ( int i = 0; i < actSiteNum; i++ ) {
            int expSiteId = expIdList.get( i );
            int actSiteId = actIdList.get( i ).intValue();
            if ( actSiteId != expSiteId ) {
                throw new Exception( "Failed to check siteId, ws = "
                        + ws.getName() + ", fileId = " + fileId.get()
                        + ", expSiteId = " + expSiteId + ", actSiteId = "
                        + actSiteId + ", expSiteIds = " + expIdList
                        + ", actSiteIds = " + actIdList );
            }
        }
    }

    /**
     * @descreption 从本地站点下载文件校验MD5
     * @param ws
     * @param fileId
     * @param localPath
     * @param filePath
     * @return
     * @throws Exception
     */
    public static void checkData( ScmWorkspace ws, ScmId fileId,
            java.io.File localPath, String filePath ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        checkData( ws, file, localPath, filePath );

    }

    /**
     * @descreption 从本地下载历史版本文件校验MD5
     * @param ws
     * @param fileId
     * @param localPath
     * @param filePath
     * @param majorVersion
     * @param minorVersion
     * @return
     * @throws Exception
     */
    public static void checkHistoryData( ScmWorkspace ws, ScmId fileId,
            java.io.File localPath, String filePath, int majorVersion,
            int minorVersion ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, majorVersion,
                minorVersion );
        checkData( ws, file, localPath, filePath );

    }

    /**
     * @descreption 校验文件数据
     * @param ws
     * @param file
     * @param localPath
     * @param filePath
     * @return
     * @throws Exception
     */
    public static void checkData( ScmWorkspace ws, ScmFile file,
            java.io.File localPath, String filePath ) throws Exception {
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContentFromLocalSite( downloadPath );
        String expMd5 = TestTools.getMD5( filePath );
        String actMd5 = TestTools.getMD5( downloadPath );
        if ( !expMd5.equals( actMd5 ) ) {
            throw new Exception(
                    "Failed to check data, " + "expMd5=" + expMd5 + ", actMd5="
                            + actMd5 + " fileId:" + file.getFileId().get() );
        }
        TestTools.LocalFile.removeFile( downloadPath );
    }

    /**
     * @descreption 校验多个文件的元数据
     * @param ws
     * @param fileIds
     * @param expSites
     * @return
     * @throws Exception
     */
    public static void checkMeta( ScmWorkspace ws, List< ScmId > fileIds,
            SiteWrapper[] expSites ) throws Exception {
        for ( int i = 0; i < fileIds.size(); i++ ) {
            checkMeta( ws, fileIds.get( i ), expSites );
        }
    }

    /**
     * @descreption 校验文件的ContentLocationsInfo
     * @param fileContentLocationsInfo
     * @param site
     * @param fileId
     * @param ws
     * @return
     * @throws Exception
     */
    public static void checkContentLocation(
            List< ScmContentLocation > fileContentLocationsInfo,
            SiteWrapper site, ScmId fileId, ScmWorkspace ws ) throws Exception {
        ScmContentLocation fileContentLocationInfo = null;
        for ( int i = 0; i < fileContentLocationsInfo.size(); i++ ) {
            if ( fileContentLocationsInfo.get( i ).getSite() == site
                    .getSiteId() ) {
                fileContentLocationInfo = fileContentLocationsInfo.get( i );
                checkSiteInfo( fileContentLocationInfo, site );
                checkFulldata( fileContentLocationInfo, site, fileId, ws );
            }
        }
        if ( fileContentLocationInfo == null ) {
            throw new Exception(
                    "the fileContentLocationsInfo don‘t have the siteInfo, site = "
                            + site.getSiteName() );
        }
    }

    /**
     * @descreption 校验文件站点
     * @param fileContentLocationInfo
     * @param site
     * @return
     * @throws Exception
     */
    public static void checkSiteInfo(
            ScmContentLocation fileContentLocationInfo, SiteWrapper site ) {
        Map< String, Object > actSiteInfo = new HashMap<>();
        Map< String, Object > expSiteInfo = new HashMap<>();
        actSiteInfo.put( "siteId", fileContentLocationInfo.getSite() );
        actSiteInfo.put( "datasourceType", fileContentLocationInfo.getType() );
        actSiteInfo.put( "urls", fileContentLocationInfo.getUrls() );

        expSiteInfo.put( "siteId", site.getSiteId() );
        expSiteInfo.put( "datasourceType", site.getDataType() );
        getSiteInfo( expSiteInfo, site );

        Assert.assertEquals( actSiteInfo.toString(), expSiteInfo.toString() );
    }

    /**
     * @descreption 校验文件ContentLocationsInfo中的Fulldata信息
     * @param fileContentLocationInfo
     * @param site
     * @param fileId
     * @param ws
     * @return
     * @throws Exception
     */
    public static void checkFulldata(
            ScmContentLocation fileContentLocationInfo, SiteWrapper site,
            ScmId fileId, ScmWorkspace ws ) throws Exception {
        Map< String, Object > actFulldata = fileContentLocationInfo
                .getFullData();
        Map< String, Object > expFulldata = new HashMap<>();

        List< ScmDataLocation > dataLocations = ws.getDataLocations();

        // 数据源类型为CEPH_S3
        if ( site.getDataType() == ScmType.DatasourceType.CEPH_S3 ) {
            expFulldata.put( "object_id", fileId.toString() );
            expFulldata.put( "bucket",
                    CephS3Utils.getBucketName( site, ws.getName() ) );

            Assert.assertEquals( actFulldata.get( "object_id" ),
                    expFulldata.get( "object_id" ), "actFulldata=" + actFulldata
                            + ", expFulldata=" + expFulldata );
            Assert.assertEquals( actFulldata.get( "bucket" ),
                    expFulldata.get( "bucket" ), "actFulldata=" + actFulldata
                            + ", expFulldata=" + expFulldata );
        }

        // 数据源类型为SEQUOIADB
        if ( site.getDataType() == ScmType.DatasourceType.SEQUOIADB ) {
            String csType = "year";
            String clType = "month";
            for ( ScmDataLocation siteLocation : dataLocations ) {

                if ( siteLocation.getSiteName().equals( site.getSiteName() ) ) {
                    Object data_sharding_type = siteLocation.getBSONObject()
                            .get( "data_sharding_type" );
                    if ( data_sharding_type != null ) {
                        if ( ( ( BSONObject ) data_sharding_type )
                                .containsField( "collection_space" ) ) {
                            csType = ( ( BSONObject ) data_sharding_type )
                                    .get( "collection_space" ).toString();
                        }

                        if ( ( ( BSONObject ) data_sharding_type )
                                .containsField( "collection" ) ) {
                            clType = ( ( BSONObject ) data_sharding_type )
                                    .get( "collection" ).toString();
                        }
                    }
                }
            }
            if ( csType != "none" ) {
                expFulldata.put( "cs", String.format( "%s_LOB_%s", ws.getName(),
                        TestSdbTools.getCsClPostfix( csType ) ) );
            } else {
                expFulldata.put( "cs",
                        String.format( "%s_LOB", ws.getName() ) );
            }

            expFulldata.put( "cl", String.format( "LOB_%s",
                    TestSdbTools.getCsClPostfix( clType ) ) );

            expFulldata.put( "lob_id", fileId.toString() );

            Assert.assertEquals( actFulldata.get( "cs" ),
                    expFulldata.get( "cs" ), "actFulldata=" + actFulldata
                            + ", expFulldata=" + expFulldata );
            Assert.assertEquals( actFulldata.get( "cl" ),
                    expFulldata.get( "cl" ), "actFulldata=" + actFulldata
                            + ", expFulldata=" + expFulldata );
            Assert.assertEquals( actFulldata.get( "lob_id" ),
                    expFulldata.get( "lob_id" ), "actFulldata=" + actFulldata
                            + ", expFulldata=" + expFulldata );
        }

        // 数据源类型为CEPH_SWIFT
        if ( site.getDataType() == ScmType.DatasourceType.CEPH_SWIFT ) {
            expFulldata.put( "object_id", fileId.toString() );
            Assert.assertEquals( actFulldata.get( "object_id" ),
                    expFulldata.get( "object_id" ), "actFulldata=" + actFulldata
                            + ", expFulldata=" + expFulldata );
        }

        // 数据源类型为HDFS或者HBASE
        if ( site.getDataType() == ScmType.DatasourceType.HDFS
                || site.getDataType() == ScmType.DatasourceType.HBASE ) {
            expFulldata.put( "file_name", fileId.toString() );
            Assert.assertEquals( actFulldata.get( "file_name" ),
                    expFulldata.get( "file_name" ), "actFulldata=" + actFulldata
                            + ", expFulldata=" + expFulldata );
        }
    }

    /**
     * @descreption 获取站点信息
     * @param siteInfo
     * @param site
     * @return
     * @throws Exception
     */
    public static void getSiteInfo( Map< String, Object > siteInfo,
            SiteWrapper site ) {
        if ( site.getDataType() == ScmType.DatasourceType.HBASE ) {
            siteInfo.put( "urls", String.format( "[%s]",
                    site.getDataDsConf().get( "hbase.zookeeper.quorum" ) ) );

        } else if ( site.getDataType() == ScmType.DatasourceType.HDFS ) {
            siteInfo.put( "urls", String.format( "[%s]",
                    site.getDataDsConf().get( "fs.defaultFS" ) ) );
        } else {
            siteInfo.put( "urls", site.getDataDsUrls() );
        }
    }

    /**
     * @descreption 根据时间生成fileId
     * @param createDate
     * @return
     * @throws Exception
     */
    public static String getFileIdByDate( Date createDate ) {
        long seconds = createDate.getTime() / 1000L;
        int inc = serial.incrementAndGet();
        if ( seconds > 253339200000L ) {
            throw new RuntimeException( "seconds is out of bounds:seconds="
                    + seconds + ",max=" + 253339200000L );
        } else {
            byte[] total = new byte[ 12 ];
            ByteBuffer bb = ByteBuffer.wrap( total );
            bb.putInt( ( int ) seconds );
            byte versionClusterId = ( byte ) ( 0 | 64 );
            bb.put( versionClusterId );
            bb.putShort( ( short ) 0 );
            bb.put( ( byte ) ( ( int ) ( seconds >> 32 ) ) );
            bb.putInt( inc );
            return byteArrayToString( total );
        }
    }

    /**
     * @descreption 字节转为字符串
     * @param b
     * @return String
     * @throws
     */
    public static String byteArrayToString( byte[] b ) {
        StringBuilder buf = new StringBuilder( 24 );

        for ( int i = 0; i < b.length; ++i ) {
            int x = b[ i ] & 255;
            String s = Integer.toHexString( x );
            if ( s.length() == 1 ) {
                buf.append( "0" );
            }
            buf.append( s );
        }
        return buf.toString();
    }

    /**
     * @descreption create File by stream
     * @param ws
     * @param fileName
     * @param data
     * @param authorName
     * @return
     * @throws ScmException
     */
    public static ScmId createFileByStream( ScmWorkspace ws, String fileName,
            byte[] data, String authorName ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        new Random().nextBytes( data );
        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        file.setMimeType( fileName + ".txt" );
        file.setCreateTime( new Date( new Date().getTime() - 1000 * 60 * 60 ) );
        ScmId fileId = file.save();
        return fileId;
    }

    /**
     * @descreption create File by stream
     * @param ws
     * @param fileName
     * @param data
     * @return
     * @throws ScmException
     */
    public static ScmId createFileByStream( ScmWorkspace ws, String fileName,
            byte[] data ) throws ScmException {
        return createFileByStream( ws, fileName, data, fileName );
    }

    /**
     * @descreption create File by the local file
     * @param ws
     * @param fileName
     * @param filePath
     * @param authorName
     * @return ScmId
     * @throws ScmException
     */
    public static ScmId create( ScmWorkspace ws, String fileName,
            String filePath, String authorName ) throws ScmException {
        ScmId fileId = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            file.setAuthor( authorName );
            file.setContent( filePath );
            fileId = file.save();
        } catch ( ScmException e ) {
            logger.error( "[test] create scmfile, fileName=" + fileName );
            e.printStackTrace();
            throw e;
        }
        return fileId;
    }

    /**
     * @descreption scm桶下创建文件，使用文件方式
     * @param bucket
     * @param fileName
     * @param filePath
     * @return fileId
     * @throws ScmException
     */
    public static ScmId createFile( ScmBucket bucket, String fileName,
            String filePath ) throws ScmException {
        ScmFile file = bucket.createFile( fileName );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setTitle( fileName );
        ScmId fileId = file.save();
        return fileId;
    }

    /**
     * @descreption scm桶下创建文件，使用文件流方式
     * @param bucket
     * @param fileName
     * @param data
     * @return
     * @throws ScmException
     */
    public static ScmId createFile( ScmBucket bucket, String fileName,
            byte[] data ) throws ScmException {
        return createFile( bucket, fileName, data, fileName );
    }

    /**
     * @descreption scm桶下创建文件，使用文件流方式
     * @param bucket
     * @param fileName
     * @param data
     * @param authorName
     * @return
     * @throws ScmException
     */
    public static ScmId createFile( ScmBucket bucket, String fileName,
            byte[] data, String authorName ) throws ScmException {
        ScmFile file = bucket.createFile( fileName );
        new Random().nextBytes( data );
        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        ScmId fileId = file.save();
        return fileId;
    }

    /**
     * @descreption scm桶下创建文件，使用字节流方式
     * @param ws
     * @param fileName
     * @param data
     * @param authorName
     * @return
     * @throws ScmException
     */
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
     * @descreption 指定fileId下载文件
     * @param fileId
     * @param siteWorkspace
     * @return 下载时间
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
     * @descreption 指定fileId下载文件失败
     * @param fileId
     * @param siteWorkspace
     * @return
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
     * @descreption 上传文件
     *
     * @param filePath
     * @param fileName
     * @param fileIdList
     * @param siteWorkspace
     * @return 上传时间
     * @throws Exception
     */
    public static long createFiles( String filePath, String fileName,
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
     * @descreption 上传文件失败
     * @param filePath
     * @param fileName
     * @param fileIdList
     * @param siteWorkspace
     * @return
     * @throws Exception
     */
    public static void createFileFialed( String filePath, String fileName,
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
     * @descreption 根据文件大小和内容生成多个文件
     * @param fileSizes
     * @param filePathList
     * @return File
     * @throws Exception
     */
    public static File createFiles( int[] fileSizes,
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
}
