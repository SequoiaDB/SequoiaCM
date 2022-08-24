package com.sequoiacm.testcommon.scmutils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;

import org.apache.commons.codec.binary.Hex;
import java.security.MessageDigest;
import java.io.*;
import java.util.*;

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
        ClientConfiguration config = new ClientConfiguration();
        config.setUseExpectContinue( false );
        config.setSocketTimeout( 300000 );
        return buildS3Client( ACCESS_KEY, SECRET_KEY, S3URL, config );
    }

    /**
     * @param ACCESS_KEY
     * @param SECRET_KEY
     * @param S3URL
     * @return
     * @descreption 使用ACCESS_KEY和SECRET_KEY连接指定站点的S3节点
     */
    public static AmazonS3 buildS3Client( String ACCESS_KEY, String SECRET_KEY,
            String S3URL, ClientConfiguration config ) {
        AWSCredentials credentials = new BasicAWSCredentials( ACCESS_KEY,
                SECRET_KEY );
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                S3URL, clientRegion );
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
        if ( s3Client.doesBucketExist( bucketName ) ) {
            String bucketVerStatus = s3Client
                    .getBucketVersioningConfiguration( bucketName ).getStatus();
            if ( bucketVerStatus == "Off" ) {
                deleteAllObjects( s3Client, bucketName );
            } else {
                deleteAllObjectVersions( s3Client, bucketName );
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
            List< String > expBucketNames, boolean sort,
            List< String > ignoreBuckets ) throws ScmException {
        try {
            List< String > actBucketNames = new ArrayList<>();
            while ( scmBucketScmCursor.hasNext() ) {
                actBucketNames.add( scmBucketScmCursor.getNext().getName() );
            }
            if ( sort ) {
                // 排序用例无法靠公共方法处理环境桶干扰，需要用例自己解决
                Assert.assertEquals( actBucketNames, expBucketNames,
                        "act:" + actBucketNames.toString() + " exp:"
                                + expBucketNames.toString() );
            } else {
                // 非排序用例可以依赖该方法处理环境桶干扰
                actBucketNames.removeAll( ignoreBuckets );
                Assert.assertEqualsNoOrder( actBucketNames.toArray(),
                        expBucketNames.toArray(),
                        "act:" + actBucketNames.toString() + " exp:"
                                + expBucketNames.toString() );
            }
        } finally {
            scmBucketScmCursor.close();
        }
    }

    public static void checkBucketList(
            ScmCursor< ScmBucket > scmBucketScmCursor,
            List< String > expBucketNames, boolean sort ) throws ScmException {
        checkBucketList( scmBucketScmCursor, expBucketNames, sort,
                new ArrayList< String >() );
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
     * set the bucket versioning status
     *
     * @param s3Client
     * @param bucketName
     * @param status:"null","Suspended","Enable"
     */
    public static void setBucketVersioning( AmazonS3 s3Client,
            String bucketName, String status ) {
        BucketVersioningConfiguration configuration = new BucketVersioningConfiguration()
                .withStatus( status );
        SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest = new SetBucketVersioningConfigurationRequest(
                bucketName, configuration );
        s3Client.setBucketVersioningConfiguration(
                setBucketVersioningConfigurationRequest );
    }

    /**
     * get the file part MD5 value
     *
     * @author wangkexin
     * @param file
     * @param offset
     *            offset value.
     * @param partSize
     *            file part size.
     * @throws IOException
     * @return md5 value
     */
    public static String getFilePartMD5( File file, long offset, long partSize )
            throws IOException {
        FileInputStream fileInputStream = null;
        int length = ( int ) file.length();
        try {
            MessageDigest md5 = MessageDigest.getInstance( "MD5" );
            fileInputStream = new FileInputStream( file );
            byte[] buffer = new byte[ length ];
            if ( fileInputStream.read( buffer ) != -1 ) {
                md5.update( buffer, ( int ) offset, ( int ) partSize );
            }
            // 文件指定部分的md5值
            return new String( Hex.encodeHex( md5.digest() ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        } finally {
            if ( fileInputStream != null ) {
                fileInputStream.close();
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
     * read the entire file length after the seek, to compare the read result
     *
     * @author wuyan
     * @param sourceFile
     * @param size
     *            seek size.
     * @param outputFile
     *            seek read than write file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void seekReadFile( String sourceFile, int size,
            String outputFile ) throws FileNotFoundException, IOException {
        try ( RandomAccessFile raf = new RandomAccessFile( sourceFile, "rw" ) ;
                OutputStream fos = new FileOutputStream( outputFile )) {
            raf.seek( size );
            int readSize;
            int off = 0;
            int len = 1024 * 1024;
            byte[] buf = new byte[ off + len ];
            while ( true ) {
                readSize = raf.read( buf, off, len );
                if ( readSize <= 0 ) {
                    break;
                }
                fos.write( buf, off, readSize );
            }
        }
    }

    /**
     * @descreption 修改桶版本控制状态
     * @param s3Client
     * @param bucketName
     * @param BucketVersionConf
     */
    public static void updateBucketVersionConfig( AmazonS3 s3Client,
            String bucketName, String BucketVersionConf ) {
        BucketVersioningConfiguration config = new BucketVersioningConfiguration()
                .withStatus( BucketVersionConf );
        s3Client.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest( bucketName,
                        config ) );
    }

    public static void updateBucketVersionConfig( String bucketName,
            String BucketVersionConf ) throws Exception {
        AmazonS3 s3Client = S3Utils.buildS3Client();
        try {
            updateBucketVersionConfig( s3Client, bucketName,
                    BucketVersionConf );
        } finally {
            s3Client.shutdown();
        }
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
     * @descreption 校验scm桶下文件内容，通过文件流方式比较
     * @param file
     *            获取文件实例
     * @param fileDatas
     *            预期数据内容
     * @throws ScmException
     */
    public static void checkFileContent( ScmFile file, byte[] fileDatas )
            throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        byte[] downloadData = outputStream.toByteArray();
        VersionUtils.assertByteArrayEqual( downloadData, fileDatas );
    }

    /**
     * @descreption 校验scm桶下文件内容，通过文件方式比较
     * @param file
     *            获取文件实例
     * @param filePath
     *            预期对比文件路径
     * @param localPath
     *            下载文件路径
     * @throws ScmException
     */
    public static void checkFileContent( ScmFile file, String filePath,
            File localPath ) throws Exception {
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ),
                "---file downLoadPath = " + downloadPath );
    }

    public static List< String > getEnvBuckets() throws Exception {
        List< String > envBuckets = new ArrayList<>();
        ScmSession session = TestScmTools.createSession();
        ScmCursor< ScmBucket > cursor = ScmFactory.Bucket.listBucket( session,
                null, null, 0, -1 );
        try {
            while ( cursor.hasNext() ) {
                envBuckets.add( cursor.getNext().getName() );
            }
        } finally {
            cursor.close();
            session.close();
        }
        return envBuckets;
    }

    public static String getRandomString( int length ) {
        String str = "adcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < length; i++ ) {
            int number = random.nextInt( str.length() );
            sb.append( str.charAt( number ) );
        }
        return sb.toString();
    }

    public static List< ScmFileBasicInfo > getVersionList( ScmSession session,
            ScmWorkspace ws, String bucketName ) throws ScmException {
        List< ScmFileBasicInfo > fileList = new ArrayList<>();
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        BSONObject orderByName = new BasicBSONObject();
        orderByName.put( ScmAttributeName.File.FILE_NAME, 1 );
        BSONObject orderByVersion = new BasicBSONObject();
        orderByVersion.put( ScmAttributeName.File.MAJOR_VERSION, -1 );
        ScmCursor< ScmFileBasicInfo > fileCursor = bucket.listFile( null,
                orderByName, 0, -1 );
        while ( fileCursor.hasNext() ) {
            List< ScmFileBasicInfo > fileVersions = new ArrayList<>();
            ScmFileBasicInfo curFile = fileCursor.getNext();
            fileList.add( curFile );
            ScmId fileId = curFile.getFileId();
            ScmCursor< ScmFileBasicInfo > versionCursor = ScmFactory.File
                    .listInstance( ws, ScmType.ScopeType.SCOPE_HISTORY,
                            ScmQueryBuilder
                                    .start( ScmAttributeName.File.FILE_ID )
                                    .is( fileId.toString() ).get(),
                            orderByVersion, 0, -1 );
            while ( versionCursor.hasNext() ) {
                fileVersions.add( versionCursor.getNext() );
            }
            Collections.sort( fileVersions,
                    new Comparator< ScmFileBasicInfo >() {
                        @Override
                        public int compare( ScmFileBasicInfo o1,
                                ScmFileBasicInfo o2 ) {
                            return o2.getVersionSerial().getMajorSerial()
                                    - o1.getVersionSerial().getMajorSerial();
                        }
                    } );
            fileList.addAll( fileVersions );
        }

        return fileList;
    }
}
