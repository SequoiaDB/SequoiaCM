package com.sequoiacm.testcommon.scmutils;

import java.util.*;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sun.org.apache.xpath.internal.objects.XString;
import org.apache.log4j.Logger;
import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScmFileUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( ScmFileUtils.class );

    public static ScmId create( ScmWorkspace ws, String fileName,
            String filePath ) throws ScmException {
        ScmId fileId = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            file.setAuthor( fileName );
            file.setContent( filePath );
            fileId = file.save();
        } catch ( ScmException e ) {
            logger.error( "[test] create scmfile, fileName=" + fileName );
            e.printStackTrace();
            throw e;
        }
        return fileId;
    }

    public static void cleanFile( WsWrapper ws, BSONObject condition )
            throws ScmException {
        cleanFile( ws.getName(), condition );
    }

    public static void cleanFile( String wsName, BSONObject condition )
            throws ScmException {
        ScmSession session = null;
        SiteWrapper site = ScmInfo.getSite();
        ScmCursor< ScmFileBasicInfo > cursor = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession( site );
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
     * @throws Exception
     */
    public static void checkMetaAndData( WsWrapper ws, ScmId fileId,
            SiteWrapper[] expSites, java.io.File localPath, String filePath )
            throws Exception {
        checkMetaAndData( ws.getName(), fileId, expSites, localPath, filePath );
    }

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
                session = TestScmTools.createSession( site );
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
                session = TestScmTools.createSession( site );
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
     * @throws Exception
     */
    public static void checkHistoryMeta( ScmWorkspace ws, ScmId fileId,
            SiteWrapper[] expSites, int majorVersion, int minorVersion )
            throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, majorVersion,
                minorVersion );
        checkMeta( ws, file, expSites );
    }

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
     * @throws ScmException
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
     * @throws Exception
     */
    public static void checkHistoryData( ScmWorkspace ws, ScmId fileId,
            java.io.File localPath, String filePath, int majorVersion,
            int minorVersion ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, majorVersion,
                minorVersion );
        checkData( ws, file, localPath, filePath );

    }

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
     * @throws Exception
     */
    public static void checkMeta( ScmWorkspace ws, List< ScmId > fileIds,
            SiteWrapper[] expSites ) throws Exception {
        for ( int i = 0; i < fileIds.size(); i++ ) {
            checkMeta( ws, fileIds.get( 0 ), expSites );
        }
    }

    /**
     * check scmfile's ContentLocation
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
            String csType = null;
            String clType = null;
            for ( ScmDataLocation siteLocation : dataLocations ) {

                if ( siteLocation.getSiteName().equals( site.getSiteName() ) ) {
                    Object data_sharding_type = siteLocation.getBSONObject()
                            .get( "data_sharding_type" );
                    if ( data_sharding_type == null ) {
                        csType = "year";
                        clType = "month";
                    } else {
                        csType = ( ( BSONObject ) data_sharding_type )
                                .get( "collection_space" ).toString();
                        clType = ( ( BSONObject ) data_sharding_type )
                                .get( "collection" ).toString();
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
}
