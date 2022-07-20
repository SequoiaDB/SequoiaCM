package com.sequoiacm.testcommon.scmutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;

public class S3Utils extends TestScmBase {
    private static String clientRegion = "us-east-1";
    private static String gateway = gateWayList.get( 0 );

    /**
     * @return
     * @throws Exception
     * @descreption 创建一个S3连接
     */
    public static AmazonS3 buildS3Client() throws Exception {
        return buildS3Client( TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey, getS3Url() );
    }

    /**
     * @param ACCESS_KEY
     * @param SECRET_KEY
     * @return
     * @throws Exception
     * @descreption 使用指定ACCESS_KEY和SECRET_KEY连接
     */
    public static AmazonS3 buildS3Client( String ACCESS_KEY, String SECRET_KEY )
            throws Exception {
        return buildS3Client( ACCESS_KEY, SECRET_KEY, getS3Url() );
    }

    /**
     * @param ACCESS_KEY
     * @param SECRET_KEY
     * @param S3URL
     * @return
     * @descreption 使用ACCESS_KEY和SECRET_KEY连接指定站点的S3节点
     */
    public static AmazonS3 buildS3Client( String ACCESS_KEY, String SECRET_KEY,
            String S3URL ) {
        AWSCredentials credentials = new BasicAWSCredentials( ACCESS_KEY,
                SECRET_KEY );
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                S3URL, clientRegion );
        ClientConfiguration config = new ClientConfiguration();
        config.setUseExpectContinue( false );
        config.setSocketTimeout( 300000 );
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration( endpointConfiguration )
                .withClientConfiguration( config )
                .withChunkedEncodingDisabled( true )
                .withPathStyleAccessEnabled( true )
                .withCredentials(
                        new AWSStaticCredentialsProvider( credentials ) )
                .build();
        return s3Client;
    }

    /**
     * @return
     * @throws Exception
     * @descreption 根据网关生成s3 url
     */
    public static String getS3Url() {
        return "http://" + gateway;
    }

    /**
     * delete buckets with same bucketNamePrefix
     *
     * @param s3Client,bucketName
     */
    public static void deleteEmptyBucketsWithPrefix( AmazonS3 s3Client,
            String bucketPrefix ) {
        List< Bucket > buckets = s3Client.listBuckets();

        for ( int i = 0; i < buckets.size(); i++ ) {
            if ( buckets.get( i ).getName().startsWith( bucketPrefix ) ) {
                s3Client.deleteBucket( buckets.get( i ).getName() );
            }
        }
    }

    /**
     * delete one bucket by bucketName
     *
     * @param s3Client,bucketName
     */
    public static void clearBucket( AmazonS3 s3Client, String bucketName ) {
        if ( s3Client.doesBucketExistV2( bucketName ) ) {
            String bucketVerStatus = s3Client
                    .getBucketVersioningConfiguration( bucketName ).getStatus();
            if ( bucketVerStatus == "Off" ) {
                deleteAllObjects( s3Client, bucketName );
            } else {
                deleteAllObjectVersions( s3Client, bucketName );
                ;
            }
            s3Client.deleteBucket( bucketName );
        }
    }

    /**
     * delete all object from the bucket
     *
     * @param s3Client
     * @param bucketName
     */
    public static void deleteAllObjects( AmazonS3 s3Client,
            String bucketName ) {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName( bucketName ).withEncodingType( "url" );
        ListObjectsV2Result result = null;
        do {
            result = s3Client.listObjectsV2( request );
            Iterator< S3ObjectSummary > objIter = result.getObjectSummaries()
                    .iterator();
            while ( objIter.hasNext() ) {
                S3ObjectSummary vs = objIter.next();
                s3Client.deleteObject( bucketName, vs.getKey() );
            }
            String continuationToken = result.getNextContinuationToken();
            request.setContinuationToken( continuationToken );
        } while ( result.isTruncated() );
    }

    /**
     * delete all object versions(required for versioned buckets)
     *
     * @param s3Client
     * @param bucketName
     */
    public static void deleteAllObjectVersions( AmazonS3 s3Client,
            String bucketName ) {
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        while ( true ) {
            Iterator< S3VersionSummary > versionIter = versionList
                    .getVersionSummaries().iterator();
            while ( versionIter.hasNext() ) {
                S3VersionSummary vs = versionIter.next();
                s3Client.deleteVersion( bucketName, vs.getKey(),
                        vs.getVersionId() );
            }

            if ( versionList.isTruncated() ) {
                versionList = s3Client.listNextBatchOfVersions( versionList );
            } else {
                break;
            }
        }
    }

    /**
     * delete all buckets
     *
     * @param s3Client
     */
    public static void clearBuckets( AmazonS3 s3Client ) {
        List< Bucket > buckets = s3Client.listBuckets();
        for ( int i = 0; i < buckets.size(); i++ ) {
            String bucketName = buckets.get( i ).getName();
            deleteAllObjects( s3Client, bucketName );
            deleteAllObjectVersions( s3Client, bucketName );
            s3Client.deleteBucket( bucketName );
        }
    }

    /**
     * download the object ,than get the object content md5
     *
     * @param s3Client
     * @param localPath
     * @param bucketName
     * @return md5
     */
    public static String getMd5OfObject( AmazonS3 s3Client, File localPath,
            String bucketName, String key ) throws Exception {
        return getMd5OfObject( s3Client, localPath, bucketName, key, null );
    }

    /**
     * download the object with versionId,than get the object content md5
     *
     * @param s3Client
     * @param localPath
     * @param bucketName
     * @param versionId
     * @return md5
     */
    public static String getMd5OfObject( AmazonS3 s3Client, File localPath,
            String bucketName, String key, String versionId ) throws Exception {
        GetObjectRequest request = new GetObjectRequest( bucketName, key,
                versionId );
        S3Object object = s3Client.getObject( request );
        S3ObjectInputStream s3is = object.getObjectContent();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        inputStream2File( s3is, downloadPath );
        s3is.close();
        String getMd5 = TestTools.getMD5( downloadPath );
        return getMd5;
    }

    /**
     * input stream to file
     *
     * @param inputStream
     * @param downloadPath
     */
    public static String inputStream2File( InputStream inputStream,
            String downloadPath ) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream( new File( downloadPath ), true );
            byte[] read_buf = new byte[ 1024 ];
            int read_len = 0;
            while ( ( read_len = inputStream.read( read_buf ) ) > -1 ) {
                fos.write( read_buf, 0, read_len );
            }
        } finally {
            if ( fos != null ) {
                fos.close();
            }
        }
        return downloadPath;
    }

    /**
     * delete the object of all versions(required for versioned buckets)
     *
     * @param s3Client
     * @param bucketName
     * @param keyName
     */
    public static void deleteObjectAllVersions( AmazonS3 s3Client,
            String bucketName, String keyName ) {
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        while ( true ) {
            Iterator< S3VersionSummary > versionIter = versionList
                    .getVersionSummaries().iterator();

            while ( versionIter.hasNext() ) {
                S3VersionSummary vs = versionIter.next();
                String getKey = vs.getKey();

                if ( getKey.equals( keyName ) ) {
                    s3Client.deleteVersion( bucketName, vs.getKey(),
                            vs.getVersionId() );
                }

            }

            if ( versionList.isTruncated() ) {
                versionList = s3Client.listNextBatchOfVersions( versionList );
            } else {
                break;
            }
        }
    }

    public static void clearOneObject( AmazonS3 s3Client, String bucketName,
            String key ) {
        if ( s3Client.doesObjectExist( bucketName, key ) ) {
            s3Client.deleteObject( bucketName, key );
        }
    }

    public static void checkListVSResults( VersionListing vsList,
            List< String > expCommonPrefixes,
            MultiValueMap< String, String > expMap ) {
        Collections.sort( expCommonPrefixes );
        List< String > actCommonPrefixes = vsList.getCommonPrefixes();
        Assert.assertEquals( actCommonPrefixes, expCommonPrefixes,
                "actCommonPrefixes = " + actCommonPrefixes.toString()
                        + ",expCommonPrefixes = "
                        + expCommonPrefixes.toString() );
        List< S3VersionSummary > vsSummaryList = vsList.getVersionSummaries();
        MultiValueMap< String, String > actMap = new LinkedMultiValueMap< String, String >();
        for ( S3VersionSummary versionSummary : vsSummaryList ) {
            actMap.add( versionSummary.getKey(),
                    versionSummary.getVersionId() );
        }
        Assert.assertEquals( actMap.size(), expMap.size(), "actMap = "
                + actMap.toString() + ",expMap = " + expMap.toString() );
        for ( Map.Entry< String, List< String > > entry : expMap.entrySet() ) {
            Assert.assertEquals( actMap.get( entry.getKey() ),
                    expMap.get( entry.getKey() ),
                    "actMap = " + actMap.toString() + ",expMap = "
                            + expMap.toString() );
        }
    }

    public static List< String > getCommPrefixes( String[] objectNames,
            String prefix, String delimiter ) {
        List< String > commPrefixes = new ArrayList< String >();
        for ( String objectName : objectNames ) {
            if ( objectName.startsWith( prefix ) ) {
                int end = objectName.indexOf( delimiter, prefix.length() );
                if ( end != -1 ) {
                    String commPrefix = objectName.substring( 0,
                            end + delimiter.length() );
                    if ( !commPrefixes.contains( commPrefix ) ) {
                        commPrefixes.add( commPrefix );
                    }
                }
            }
        }
        return commPrefixes;
    }

    public static List< String > getKeys( String[] objectNames, String prefix,
            String delimiter ) {
        List< String > keys = new ArrayList< String >();
        for ( String objectName : objectNames ) {
            if ( objectName.startsWith( prefix ) ) {
                int index = objectName.indexOf( delimiter, prefix.length() );
                if ( index == -1 ) {
                    keys.add( objectName );
                }
            }
        }
        return keys;
    }

    public static void checkListObjectsV2Commprefixes(
            List< String > resultList, List< String > expresultList ) {
        Collections.sort( expresultList );
        Assert.assertEquals( resultList.size(), expresultList.size(),
                "The expected results do not match the actual number of returns" );
        for ( int i = 0; i < resultList.size(); i++ ) {
            Assert.assertEquals( resultList.get( i ), expresultList.get( i ),
                    "commonPrefixes is wrong" );
        }
    }

    public static void checkListObjectsV2KeyName(
            List< S3ObjectSummary > objectSummaries,
            List< String > expresultList ) {
        Collections.sort( expresultList );
        Assert.assertEquals( objectSummaries.size(), expresultList.size(),
                "The number of returned results is wrong" );
        for ( int i = 0; i < objectSummaries.size(); i++ ) {
            Assert.assertEquals( objectSummaries.get( i ).getKey(),
                    expresultList.get( i ), "keyName is wrong" );
        }
    }

    /**
     * @param ws
     * @param objectKey
     * @return
     * @throws ScmException
     * @descreption objectKey映射fileName时，根据object查询映射scm文件的ID
     */
    public static ScmId queryS3Object( ScmWorkspace ws, String objectKey )
            throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( objectKey ).get();
        ScmCursor< ScmFileBasicInfo > scmFileBasicInfoScmCursor = ScmFactory.File
                .listInstance( ws, ScmType.ScopeType.SCOPE_CURRENT, cond );
        ScmId fileId = null;
        while ( scmFileBasicInfoScmCursor.hasNext() ) {
            fileId = scmFileBasicInfoScmCursor.getNext().getFileId();
        }
        scmFileBasicInfoScmCursor.close();
        return fileId;
    }

    public static void checkBucketList(
            ScmCursor< ScmBucket > scmBucketScmCursor,
            List< String > expBucketNames, boolean sort ) throws ScmException {
        try {
            List< String > actBucketNames = new ArrayList<>();
            while ( scmBucketScmCursor.hasNext() ) {
                actBucketNames.add( scmBucketScmCursor.getNext().getName() );
            }
            if ( sort ) {
                Assert.assertEquals( actBucketNames, expBucketNames );
            } else {
                Assert.assertEqualsNoOrder( actBucketNames.toArray(),
                        expBucketNames.toArray() );
            }
        } finally {
            scmBucketScmCursor.close();
        }
    }

    /**
     * @param session
     * @param bucketName
     * @throws ScmException
     * @descreption 使用scm api清理桶
     */
    public static void clearBucket( ScmSession session, String bucketName )
            throws ScmException {
        clearBucket( session, s3WorkSpaces, bucketName );
    }

    public static void clearBucket( ScmSession session, String wsName,
            String bucketName ) throws ScmException {
        ScmCursor< ScmFileBasicInfo > scmFileBasicInfoScmCursor = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            scmFileBasicInfoScmCursor = bucket.listFile( null, null, 0, -1 );
            while ( scmFileBasicInfoScmCursor.hasNext() ) {
                ScmId fileId = scmFileBasicInfoScmCursor.getNext().getFileId();
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            ScmFactory.Bucket.deleteBucket( session, bucketName );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.BUCKET_NOT_EXISTS ) ) {
                throw e;
            }
        } finally {
            if ( scmFileBasicInfoScmCursor != null ) {
                scmFileBasicInfoScmCursor.close();
            }
        }
    }

    /**
     * @param session
     * @return
     * @throws ScmException
     * @Descreption 获取所有s3节点名
     */
    public static List< String > getS3ServiceName( ScmSession session )
            throws ScmException {
        List< String > s3ServcieNames = new ArrayList<>();
        List< String > serviceList = ScmSystem.ServiceCenter
                .getServiceList( session );
        for ( String serviceName : serviceList ) {
            if ( serviceName.contains( "-s3" ) ) {
                s3ServcieNames.add( serviceName );
            }
        }
        return s3ServcieNames;
    }

    /**
     * @descreption 修改桶版本控制状态
     * @param s3Client
     * @param bucketName
     * @param BucketVersionConf
     */
    public static void updateBucketVersionConfig( AmazonS3 s3Client, String bucketName,
            String BucketVersionConf ) {
        BucketVersioningConfiguration config = new BucketVersioningConfiguration()
                .withStatus( BucketVersionConf );
        s3Client.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest( bucketName,
                        config ) );
    }
}
