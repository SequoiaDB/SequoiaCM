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
 * @Descreption SCM-3307:指定ifNoneMatch和ifModifiedSince条件获取对象，不匹配ifNoneMatch（标准模式）
 * @Author YiPan
 * @Date 2020/3/11
 */
public class GetObject3307 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3307";
    private String objectName = "object3307";
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private List< PutObjectResult > objectVSList = new ArrayList< PutObjectResult >();
    private int fileNum = 3;
    private Calendar cal = Calendar.getInstance();

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

        // get current eTag
        int currIndex = fileNum - 1;
        String currETag = objectVSList.get( currIndex ).getETag();

        // get object by NonMatchingETag and modified
        // the object has been modified since now-one_month
        cal.set( Calendar.DAY_OF_MONTH, cal.get( Calendar.DAY_OF_MONTH ) - 1 );
        S3Object currObject = s3Client
                .getObject( new GetObjectRequest( bucketName, objectName )
                        .withNonmatchingETagConstraint( currETag )
                        .withModifiedSinceConstraint( cal.getTime() ) );
        Assert.assertNull( currObject );
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
