package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @descreption SCM-4722 ::
 *              带prefix和delimiter查询对象版本列表，存在不匹配delimiter且最新记录为deletemarker的对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4722 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4722";
    private String[] objectNames = { "dir4722/test1/1.txt",
            "dir4722/test2/2.txt", "dir4722/test3/3.txt", "dir4722/test4",
            "dir4722/test5", "4722test6" };
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private int versionNum = 2;
    private File localPath = null;
    private List< String > filePathList = new ArrayList<>();

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

        // delete object "dir4722/test5"
        s3Client.deleteObject( bucketName, objectNames[ 4 ] );
    }

    @Test
    private void test() {
        String prefix = "dir4722/";
        String delimiter = "/";
        VersionListing vsList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withPrefix( prefix ).withDelimiter( delimiter ) );
        List< String > expCommPrefixes = S3Utils.getCommPrefixes( objectNames,
                prefix, delimiter );
        List< String > versionKeys = S3Utils.getKeys( objectNames, prefix,
                delimiter );
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap<>();
        for ( String key : versionKeys ) {
            for ( int i = versionNum; i > 0; i-- ) {
                expMap.add( key, i + ".0" );
                expMap.add( key, "false" );
            }
            if ( key.equals( objectNames[ 4 ] ) ) {
                expMap.add( key, ( versionNum + 1 ) + ".0" );
                expMap.add( key, "true" );
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        checkListVSResults( vsList, expCommPrefixes, expMap );
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

    private void checkListVSResults( VersionListing vsList,
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
            actMap.add( versionSummary.getKey(),
                    String.valueOf( versionSummary.isDeleteMarker() ) );
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
}
