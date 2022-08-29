package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Date;

/**
 * @Description SCM-4624 :: 开启版本控制，增加同名对象
 * @author wuyan
 * @Date 2022.07.05
 * @version 1.00
 */
public class Object4624 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = null;
    private String keyName = "object4624";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        bucketName = TestScmBase.enableVerBucketName;
        TestTools.LocalFile.createFile( updatePath, updateSize );
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        updateObjectWithSameContent();
        updateObjectWithDiffContent();
        checkObjectContent();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        keyName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void updateObjectWithSameContent() {
        S3Object object = s3Client.getObject( bucketName, keyName );
        Date createDate = object.getObjectMetadata().getLastModified();

        PutObjectResult result = s3Client.putObject( bucketName, keyName,
                new File( filePath ) );
        String updateVersionId = "2.0";
        Assert.assertEquals( result.getVersionId(), updateVersionId );

        // check the modify date
        S3Object updateObject = s3Client.getObject( bucketName, keyName );
        Date updateDate = updateObject.getObjectMetadata().getLastModified();
        Assert.assertFalse( updateDate.before( createDate ),
                "updateDate must be grater than createDate! " + "updateDate:"
                        + updateDate + "\t createDate:" + createDate );
    }

    private void updateObjectWithDiffContent() {
        PutObjectResult result = s3Client.putObject( bucketName, keyName,
                new File( updatePath ) );
        // check the versionId, should be 2
        Assert.assertEquals( result.getVersionId(), "3.0" );
    }

    private void checkObjectContent() throws Exception {
        String createVersionId = "1.0";
        String updateVersionId = "2.0";

        // check the content of the create object
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, createVersionId );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );

        // check the content of the first update
        String updateMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, updateVersionId );
        Assert.assertEquals( updateMd5, TestTools.getMD5( filePath ) );

        // check the content of the second update
        String secUdateMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( secUdateMd5, TestTools.getMD5( updatePath ) );
    }
}
