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
 * @Descreption SCM-3301:指定ifUnModifiedSince和ifModifiedSince条件获取对象，不匹配ifUnModifiedSince（标准模式）
 * @Author YiPan
 * @Date 2020/3/11
 */
public class GetObject3301 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3301";
    private String objectName = "object3301";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 20;
    private int updateSize = 1024 * 15;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        s3Client.putObject( bucketName, objectName, new File( filePath ) );
        Date createDate = getCreateDate( bucketName );
        s3Client.putObject( bucketName, objectName, new File( updatePath ) );

        // set date one day less than create time
        long timestamp = createDate.getTime() - 96784000l;
        Date unModifydate = new Date( timestamp );
        GetObjectRequest request = new GetObjectRequest( bucketName,
                objectName );
        request.withUnmodifiedSinceConstraint( unModifydate )
                .withModifiedSinceConstraint( createDate );
        S3Object object = s3Client.getObject( request );

        Assert.assertNull( object, "does not match object!" );
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

    private Date getCreateDate( String bucketName ) {
        S3Object object = s3Client.getObject( bucketName, objectName );
        ObjectMetadata metadata = object.getObjectMetadata();
        Date date = metadata.getLastModified();
        return date;
    }
}
