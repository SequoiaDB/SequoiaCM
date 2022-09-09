package com.sequoiacm.testcommon.dsutils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

public class CephS3Utils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( CephS3Utils.class );

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

    /*
     * public static int readContent(SiteWrapper site, WsWrapper ws, ScmId
     * fileId, String filePath) throws Exception { S3Object obj = null; int
     * readLen = 0; long fileSize = new File(filePath).length(); try { obj =
     * getS3Object(fileId, site, ws); ObjectMetadata objMetadata =
     * obj.getObjectMetadata(); long objSize = objMetadata.getContentLength();
     * if (fileSize != objSize) { throw new fileSize + "fileId = " + fileId); }
     * S3ObjectInputStream objContent = obj.getObjectContent(); byte[] buff =
     * new byte[204800]; int currentPosition = 0; boolean isEof = false; readLen
     * = objContent.read(buff, 0, 204800); if (readLen == -1) { isEof = true; }
     * currentPosition += readLen; } catch (Exception e) {
     * logger.error("failed to read content in ceph-s3, fileId = " + fileId +
     * ", siteUrl = " + site.getDataDsUrl() + "ws = " + ws.getName()); throw e;
     * } return readLen; }
     */

    private static String getBucketName( SiteWrapper site, WsWrapper ws ) {
        String bucketName = "";

        // get bucketName prefix
        String containerPrefix = ws.getContainerPrefix( site.getSiteId() );
        if ( null == containerPrefix ) {
            containerPrefix = ws.getName() + "-scmfile";
        }

        // get bucketName postFix
        String dstType = ws.getDataShardingTypeForOtherDs( site.getSiteId() );
        if ( null == dstType ) {
            dstType = "month";
        }
        String postFix = TestSdbTools.getCsClPostfix( dstType );

        // get bucketName
        if ( !dstType.equals( "none" ) ) {
            containerPrefix += "-";
        }
        bucketName = ( containerPrefix + postFix ).toLowerCase()
                .replaceAll( "_", "-" );

        return bucketName;
    }

    private static String getBucketName( SiteWrapper site, String wsName )
            throws ScmException {
        String bucketName = "";

        // get bucketName prefix
        Object containerPrefix = TestSdbTools
                .getContainerPrefix( site.getSiteId(), wsName );
        if ( null == containerPrefix ) {
            containerPrefix = wsName + "-scmfile";
        } else {
            containerPrefix = containerPrefix.toString();
        }

        // get bucketName postFix
        String dstType = TestSdbTools
                .getDataShardingTypeForOtherDs( site.getSiteId(), wsName );
        if ( null == dstType ) {
            dstType = "month";
        }
        String postFix = TestSdbTools.getCsClPostfix( dstType );

        // get bucketName
        if ( !dstType.equals( "none" ) ) {
            containerPrefix += "-";
        }
        bucketName = ( containerPrefix + postFix ).toLowerCase()
                .replaceAll( "_", "-" );

        return bucketName;
    }

}
