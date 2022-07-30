package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4705 ::
 *              带prefix、keyMarker、versionIdMarker和delimiter匹配查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4705 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4705";
    // please sort in an ascending order by objectName
    private String[] objectNames = { "air14705", "dir2/4705A.png",
            "dir3/4705.xml", "test4705.doc" };
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private int versionNum = 5;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < versionNum; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + ( fileSize + i ) + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize + i );
            filePathList.add( filePath );
        }
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( String objectName : objectNames ) {
            for ( int i = 0; i < versionNum; i++ ) {
                s3Client.putObject( new PutObjectRequest( bucketName,
                        objectName, new File( filePathList.get( i ) ) ) );
            }
        }
    }

    @Test
    private void test() throws Exception {
        String prefix = "dir";
        String delimiter = "/";
        String keyMarker = objectNames[ 0 ];

        // expected results
        List< String > expCommPrefixes = S3Utils.getCommPrefixes( objectNames,
                prefix, delimiter );

        // list versions by prefix/delimiter/currentversionId/key
        String versionIdMarker1 = "5.0";
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withDelimiter( delimiter )
                .withPrefix( prefix ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker1 ) );

        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, expCommPrefixes,
                new LinkedMultiValueMap< String, String >() );

        // list versions by prefix/delimiter/histversionId/key
        String versionIdMarker2 = "3.0";
        VersionListing vsList1 = s3Client
                .listVersions( new ListVersionsRequest()
                        .withBucketName( bucketName ).withDelimiter( delimiter )
                        .withPrefix( prefix ).withKeyMarker( keyMarker )
                        .withVersionIdMarker( versionIdMarker2 ) );

        // check
        Assert.assertEquals( vsList1.isTruncated(), false,
                "vsList1.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList1, expCommPrefixes,
                new LinkedMultiValueMap< String, String >() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
