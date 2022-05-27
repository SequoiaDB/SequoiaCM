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

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmNodeInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmTools;
import org.apache.commons.beanutils.PropertyUtilsBean;
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
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;

public class S3Utils extends TestScmBase {
    private static String clientRegion = "us-east-1";
    private static String gateway = gateWayList.get( 0 );

    /**
     * @descreption 随机连接一个s3节点
     * @return
     * @throws Exception
     */
    public static AmazonS3 buildS3Client() throws Exception {
        return buildS3Client( TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey, getS3Url() );
    }

    /**
     * @descreption 使用ACCESS_KEY和SECRET_KEY随机连接一个节点
     * @param ACCESS_KEY
     * @param SECRET_KEY
     * @return
     * @throws Exception
     */
    public static AmazonS3 buildS3Client( String ACCESS_KEY, String SECRET_KEY )
            throws Exception {
        return buildS3Client( ACCESS_KEY, SECRET_KEY, getS3Url() );
    }

    /**
     * @descreption 连接指定站点的s3节点
     * @param siteName
     * @return
     */
    public static AmazonS3 buildS3Client( String siteName ) {
        return buildS3Client( TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey, getS3UrlBySite( siteName ) );
    }

    /**
     * @descreption 使用ACCESS_KEY和SECRET_KEY连接指定站点的S3节点
     * @param ACCESS_KEY
     * @param SECRET_KEY
     * @param S3URL
     * @return
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
     * @descreption 根据站点名生成s3协议url
     * @param siteName
     * @return
     */
    public static String getS3UrlBySite( String siteName ) {
        return "http://" + gateway + "/" + siteName + "-s3";
    }

    /**
     * @descreption 随机获取一个s3节点生成url
     * @return
     * @throws Exception
     */
    public static String getS3Url() throws Exception {
        ScmSession session = TestScmTools
                .createSession( ScmInfo.getRootSite() );
        try {
            List< String > serviceList = ScmSystem.ServiceCenter
                    .getServiceList( session );
            for ( String serviceName : serviceList ) {
                if ( serviceName.contains( "-s3" ) ) {
                    return "http://" + gateway + "/" + serviceName;
                }
            }
        } finally {
            session.close();
        }
        throw new Exception( "no s3 nodes available" );
    }

    /**
     * delete one bucket by bucketName
     *
     * @param s3Client,bucketName
     */
    @SuppressWarnings("deprecation")
    public static void clearBucket( AmazonS3 s3Client, String bucketName ) {
        if ( s3Client.doesBucketExist( bucketName ) ) {
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
}
