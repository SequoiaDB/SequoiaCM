package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @Descreption SCM-3297:指定ifMatch和ifNoneMatch条件，不带versionId获取对象，不匹配ifMatch（标准模式）
 * @Author YiPan
 * @Date 2020/3/11
 */
public class GetObject3297 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3297";
    private String objectName = "object3297";
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private List< PutObjectResult > objectVSList = new ArrayList< PutObjectResult >();
    private int fileNum = 10;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        String filePath = null;
        for ( int i = 0; i < fileNum; i++ ) {
            filePath = localPath + File.separator + "localFile_"
                    + ( fileSize + i ) + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize + i );
            filePathList.add( filePath );
        }
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        // create multiple versions object in the bucket
        for ( int i = 0; i < fileNum; i++ ) {
            objectVSList
                    .add( s3Client.putObject( new PutObjectRequest( bucketName,
                            objectName, new File( filePathList.get( i ) ) ) ) );
        }

        String wrongETag = objectVSList.get( fileNum - 2 ).getETag();
        String currETag = objectVSList.get( fileNum - 1 ).getETag();

        // get object by eTag
        S3Object object = s3Client
                .getObject( new GetObjectRequest( bucketName, objectName )
                        .withMatchingETagConstraint( wrongETag )
                        .withNonmatchingETagConstraint( currETag ) );
        Assert.assertNull( object );
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
