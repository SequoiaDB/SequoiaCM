package com.sequoiacm.testcommon.dsutils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.testng.Assert;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;

public class CephS3Utils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( CephS3Utils.class );

    /**
     * @descreption 创建CephS3连接
     * @param site
     * @return AmazonS3
     */
    public static AmazonS3 createConnect(
            SiteWrapper site ) /* throws ScmCryptoException */ {
        AmazonS3 conn = null;
        int siteId = site.getSiteId();
        String siteDsUrl = site.getDataDsUrl();
        String siteDsUser = site.getDataUser();
        // String siteDsPasswd = site.getDataPasswd();
        String siteDsPasswd = TestScmBase.CEPHS3_s3SecretKey;
        try {
            AWSCredentials awsCredentials = new BasicAWSCredentials( siteDsUser,
                    siteDsPasswd );

            ClientConfiguration conf = new ClientConfiguration();
            conf.setProtocol( Protocol.HTTP );
            conf.setSignerOverride( "S3SignerType" );
            conn = new AmazonS3Client( awsCredentials, conf );

            S3ClientOptions s3ClientOpt = new S3ClientOptions();
            s3ClientOpt.setPathStyleAccess( true );
            conn.setS3ClientOptions( s3ClientOpt );
            conn.setEndpoint( siteDsUrl );
        } catch ( Exception e ) {
            logger.error( "failed to connect ceph-s3, siteId = " + siteId );
            throw e;
        }
        return conn;
    }

    /**
     * @descreption 指定cephS3站点创建Object
     * @param site
     * @param ws
     * @param fileId
     * @param filePath
     * @return
     */
    public static void putObject( SiteWrapper site, WsWrapper ws, ScmId fileId,
            String filePath ) throws Exception {
        AmazonS3 conn = null;
        String bucketName = "";
        String key = fileId.get();
        List< PartETag > tags = new ArrayList< PartETag >();
        try {
            conn = createConnect( site );

            bucketName = getBucketName( site, ws );

            conn.createBucket( bucketName );

            InitiateMultipartUploadRequest initReq = new InitiateMultipartUploadRequest(
                    bucketName, key );
            InitiateMultipartUploadResult initResult = conn
                    .initiateMultipartUpload( initReq );
            String uploadId = initResult.getUploadId();

            File file = new File( filePath );
            int fileSize = ( int ) file.length();
            int partNum = 1;
            byte[] data = TestTools.getBuffer( filePath );
            ByteArrayInputStream inputStream = new ByteArrayInputStream( data );
            UploadPartRequest uploadPartReq = new UploadPartRequest()
                    .withBucketName( bucketName ).withInputStream( inputStream )
                    .withKey( key ).withPartNumber( partNum )
                    .withPartSize( fileSize ).withUploadId( uploadId );
            UploadPartResult uploadPartResult = conn
                    .uploadPart( uploadPartReq );

            PartETag partETag = uploadPartResult.getPartETag();
            tags.add( partETag );

            CompleteMultipartUploadRequest compReq = new CompleteMultipartUploadRequest(
                    bucketName, key, uploadId, tags );
            conn.completeMultipartUpload( compReq );
        } catch ( AmazonClientException e ) {
            logger.error( "failed to write file in ceph-s3, fileId = "
                    + fileId.get() );
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * @descreption 从cephS3站点获取对象数据和元数据
     * @param site
     * @param ws
     * @param fileId
     * @param filePath
     * @param downloadPath
     * @return
     */
    public static ObjectMetadata getObjMetadata( SiteWrapper site, WsWrapper ws,
            ScmId fileId, String filePath, String downloadPath )
            throws Exception {
        AmazonS3 conn = null;
        String key = fileId.get();
        ObjectMetadata obj = null;
        long fileSize = new File( filePath ).length();
        try {
            conn = createConnect( site );
            String bucketName = getBucketName( site, ws );
            obj = conn.getObject( new GetObjectRequest( bucketName, key ),
                    new File( downloadPath ) );

            // check object length
            long objLen = obj.getContentLength();
            if ( fileSize != objLen ) {
                throw new Exception( "objLength is error, objSize = " + objLen
                        + "fileSize = " + fileSize + ", fileId = " + fileId );
            }

            // check object content
            String expMd5 = TestTools.getMD5( filePath );
            String actMd5 = TestTools.getMD5( downloadPath );
            if ( expMd5.equals( actMd5 ) ) {
                throw new Exception( "objContent is error, actMd5 = " + actMd5
                        + "expMd5 = " + expMd5 + ", fileId = " + fileId );
            }
        } catch ( Exception e ) {
            logger.error(
                    "failed to get object in ceph-s3, fileId = " + fileId );
            throw e;
        }
        return obj;
    }

    /**
     * @descreption 从cephS3站点删除桶
     * @param site
     * @param wsName
     * @return
     */
    public static void deleteBucket( SiteWrapper site, String wsName )
            throws Exception {
        AmazonS3 conn;
        String bucketName = "";
        List< Bucket > buckets;
        try {
            bucketName = getBucketName( site, wsName );

            conn = CephS3Utils.createConnect( site );
            buckets = conn.listBuckets();
            for ( int i = 0; i < buckets.size(); i++ ) {
                String tmpBucketName = buckets.get( i ).getName();
                if ( tmpBucketName.equals( bucketName ) ) {
                    ObjectListing objects = conn.listObjects( tmpBucketName );
                    List< S3ObjectSummary > summarys = objects
                            .getObjectSummaries();
                    for ( S3ObjectSummary summ : summarys ) {
                        String key = summ.getKey();
                        conn.deleteObject( tmpBucketName, key );
                    }
                    conn.deleteBucket( tmpBucketName );
                    break;
                }
            }
        } catch ( Exception e ) {
            logger.error( "failed to delete all buckets, siteId = "
                    + site.getSiteId() + ", wsName = " + wsName
                    + ", bucketName = " + bucketName );
            throw e;
        }
    }

    /**
     * @descreption 从cephS3站点删除对象
     * @param site
     * @param ws
     * @param fileId
     * @return
     */
    public static void deleteObject( SiteWrapper site, WsWrapper ws,
            ScmId fileId ) throws Exception {
        AmazonS3 conn = null;
        String bucketName = "";
        String key = fileId.get();
        try {
            conn = createConnect( site );

            bucketName = getBucketName( site, ws );
            // System.out.println("list objects before delete, objects = " +
            // listAllObjects(site, bucketName));

            conn.deleteObject( new DeleteObjectRequest( bucketName, key ) );
            // System.out.println("list objects after delete, objects = " +
            // listAllObjects(site, bucketName));
        } catch ( Exception e ) {
            logger.error( "failed to delete object in ceph-s3, fileId = "
                    + fileId.get() );
            throw e;
        }
    }

    /**
     * @descreption 从cephS3站点获取工作区对应桶名
     * @param site
     * @param ws
     * @return String
     */
    private static String getBucketName( SiteWrapper site, WsWrapper ws )
            throws ScmException {
        return getBucketName( site, ws.getName() );
    }

    /**
     * @descreption 从cephS3站点获取工作区对应桶名
     * @param site
     * @param wsName
     * @return String
     */
    public static String getBucketName( SiteWrapper site, String wsName )
            throws ScmException {
        String bucketName = "";

        // get bucketName postFix
        String dstType = TestSdbTools
                .getDataShardingTypeForOtherDs( site.getSiteId(), wsName );
        if ( null == dstType ) {
            dstType = "month";
        }
        String postFix = TestSdbTools.getCsClPostfix( dstType );

        // get bucketName prefix
        Object containerPrefix = TestSdbTools
                .getContainerPrefix( site.getSiteId(), wsName );
        if ( null == containerPrefix ) {
            containerPrefix = wsName + "-scmfile";
            if ( !dstType.equals( "none" ) ) {
                containerPrefix += "-";
            }
        } else {
            containerPrefix = containerPrefix.toString();
        }

        bucketName = ( containerPrefix + postFix ).toLowerCase()
                .replaceAll( "_", "-" );

        return bucketName;
    }

    /**
     * @descreption 获取ObjectID
     * @param wsName
     * @param objectShardingType
     * @param fileID
     * @return String
     */
    public static String getObjectID( String wsName,
            ScmShardingType objectShardingType, ScmId fileID ) {
        String objectID;
        String dstType = null;
        if ( ScmShardingType.YEAR == objectShardingType ) {
            dstType = "year";
        }

        if ( ScmShardingType.QUARTER == objectShardingType ) {
            dstType = "quarter";
        }

        if ( ScmShardingType.MONTH == objectShardingType ) {
            dstType = "month";
        }

        if ( ScmShardingType.DAY == objectShardingType ) {
            dstType = "day";
        }
        if ( ScmShardingType.NONE == objectShardingType ) {
            return fileID.toString();
        }
        objectID = String.format( "%s/%s/%s", wsName,
                TestSdbTools.getCsClPostfix( dstType ), fileID.toString() );
        return objectID;
    }

    /**
     * @descreption ceph数据源创建工作区
     * @param session
     * @param wsName
     * @param cephS3DataLocation
     * @return ScmWorkspace
     */
    public static ScmWorkspace createWS( ScmSession session, String wsName,
            ScmCephS3DataLocation cephS3DataLocation )
            throws ScmException, InterruptedException {

        // DataLocation数据源参数配置
        SiteWrapper rootSite = ScmInfo.getRootSite();
        String domainName = TestSdbTools
                .getDomainNames( rootSite.getMetaDsUrl() ).get( 0 );
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        ScmSdbDataLocation rootSiteDataLocation = new ScmSdbDataLocation(
                rootSite.getSiteName(), domainName );

        scmDataLocationList.add( cephS3DataLocation );
        scmDataLocationList.add( rootSiteDataLocation );

        // MetaLocation参数配置
        ScmSdbMetaLocation scmSdbMetaLocation = new ScmSdbMetaLocation(
                rootSite.getSiteName(), ScmShardingType.YEAR, domainName );

        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( scmDataLocationList );
        conf.setMetaLocation( scmSdbMetaLocation );
        conf.setName( wsName );
        ScmWorkspace cephWs = ScmFactory.Workspace.createWorkspace( session,
                conf );
        return cephWs;
    }

    /**
     * @descreption 给管理员用户赋予工作区操作权限
     * @param session
     * @param wsName
     * @param privilege
     * @return ScmWorkspace
     */
    public static void wsSetPriority( ScmSession session, String wsName,
            ScmPrivilegeType privilege ) throws Exception {
        ScmUser superuser = ScmFactory.User.getUser( session,
                TestScmBase.scmUserName );
        ScmResource rs = ScmResourceFactory.createWorkspaceResource( wsName );
        ScmFactory.Role.grantPrivilege( session,
                superuser.getRoles().iterator().next(), rs, privilege );
    }

    /**
     * @descreption 清理ceph数据源中桶
     * @param s3connect
     * @param bucketName
     * @return
     */
    public static void deleteBucket( AmazonS3 s3connect, String bucketName ) {
        ObjectListing objects = s3connect.listObjects( bucketName );
        String objectID;
        do {
            for ( S3ObjectSummary objectSummary : objects
                    .getObjectSummaries() ) {
                objectID = objectSummary.getKey();
                s3connect.deleteObject( bucketName, objectID );
            }
        } while ( objects.isTruncated() );
        s3connect.deleteBucket( bucketName );
    }

    /**
     * @descreption ceph数据源中创建用户和指定access_key和secret_key创建ceph_S3 key
     * @param site
     * @param userUid
     * @param access_key
     * @param secret_key
     * @param isPrimary
     *            isPrimary为ture则链接主库创建用户，为false则链接备库创建用户
     *            isPrimary为ture则链接主库创建用户，为false则链接备库创建用户
     * @throws Exception
     */
    public static void createCephS3UserAndKey( SiteWrapper site, String userUid,
            String access_key, String secret_key, Boolean isPrimary )
            throws Exception {
        Ssh ssh = null;
        String hostStr = null;
        if ( isPrimary ) {
            hostStr = site.getCephPrimaryDataDsUrl();
        } else {
            hostStr = site.getCephStandbyDataDsUrl();
        }
        String host = getHostForSiteUrl( hostStr );

        try {
            ssh = new Ssh( host );
            ssh.exec( "radosgw-admin user create --uid=" + userUid
                    + " --display-name=\"testUser\"" );
            ssh.exec( "radosgw-admin key create --uid=" + userUid
                    + " --key-type=s3 --access-key " + access_key
                    + " --secret-key " + secret_key );
        } finally {
            if ( ssh != null ) {
                ssh.disconnect();
            }
        }
    }

    /**
     * @descreption ceph数据源中删除用户
     * @param site
     * @param userUid
     * @param isPrimary
     *            isPrimary为ture则删除主库用户，为false则删除备库用户
     * @throws Exception
     */
    public static void deleteCephS3User( SiteWrapper site, String userUid,
            Boolean isPrimary ) throws Exception {
        Ssh ssh = null;
        String hostStr = null;
        if ( isPrimary ) {
            hostStr = site.getCephPrimaryDataDsUrl();
        } else {
            hostStr = site.getCephStandbyDataDsUrl();
        }

        String host = getHostForSiteUrl( hostStr );

        try {
            ssh = new Ssh( host );
            ssh.exec( "radosgw-admin user rm --uid=" + userUid
                    + " --purge-data" );
        } catch ( Exception e ) {
            if ( !e.getMessage().contains( "user does not exist" ) ) {
                throw e;
            }
        } finally {
            if ( ssh != null ) {
                ssh.disconnect();
            }
        }
    }

    /**
     * @descreption 根据站点dataDsUrl获取host
     * @param siteDataDsUrl
     * @throws Exception
     */
    public static String getHostForSiteUrl( String siteDataDsUrl ) {
        String[] split = siteDataDsUrl.split( "//" );
        String[] split1 = split[ 1 ].split( ":" );
        return split1[ 0 ];
    }

    /**
     * @descreption 在ScmInstallDir路径下创建密码文件
     * @param site
     * @param passwdPath
     * @param filename
     * @throws Exception
     */
    public static String preparePasswdFile( SiteWrapper site, String passwdPath,
            String filename ) throws Exception {
        Ssh ssh = null;
        String host = site.getNode().getHost();
        try {
            ssh = new Ssh( host );
            String remotePath = ssh.getScmInstallDir() + filename;
            ssh.scpTo( passwdPath, remotePath );
            return remotePath;
        } finally {
            if ( ssh != null ) {
                ssh.disconnect();
            }
        }
    }

    /**
     * @descreption删除指定路径下密码文件
     * @param site
     * @param remotePath
     * @throws Exception
     */
    public static void deletePasswdFile( SiteWrapper site, String remotePath )
            throws Exception {
        Ssh ssh = null;
        String host = site.getNode().getHost();
        try {
            ssh = new Ssh( host );
            ssh.exec( "rm -rf " + remotePath );
        } finally {
            if ( ssh != null ) {
                ssh.disconnect();
            }
        }
    }

    /**
     * @descreption 清理CephS3数据源中的所有桶
     * @param site
     * @throws Exception
     */
    public static void deleteAllBuckets( SiteWrapper site ) throws Exception {
        AmazonS3 conn;
        String bucketName = "";
        String key = "";
        List< Bucket > buckets;
        try {
            conn = CephS3Utils.createConnect( site );
            buckets = conn.listBuckets();
            for ( int i = 0; i < buckets.size(); i++ ) {
                bucketName = buckets.get( i ).getName();
                ObjectListing objects = conn.listObjects( bucketName );
                List< S3ObjectSummary > summarys = objects.getObjectSummaries();
                for ( S3ObjectSummary summ : summarys ) {
                    key = summ.getKey();
                    conn.deleteObject( bucketName, key );
                }
                conn.deleteBucket( bucketName );
            }

            buckets = conn.listBuckets();
            if ( 0 != buckets.size() ) {
                throw new Exception(
                        "failed to delete all buckets, remain buckets = "
                                + buckets );
            }
        } catch ( Exception e ) {
            logger.error( "failed to delete all buckets, siteId = "
                    + site.getSiteId() + ", bucketName = " + bucketName
                    + ", key = " + key );
            throw e;
        }
    }
}
