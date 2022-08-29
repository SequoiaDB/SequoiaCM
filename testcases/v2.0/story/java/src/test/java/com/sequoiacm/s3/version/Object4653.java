package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @descreption SCM-4653 ::
 *              指定ifNoneMatch/ifMatch/ifModifiedSince/ifNoneModifiedSince条件获取对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4653 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = null;
    private String objectName = "object4653";
    private AmazonS3 s3Client = null;
    private int fileSize = 3;
    private File localPath = null;
    private List< String > filePathList = new ArrayList<>();
    private List< PutObjectResult > objectVSList = new ArrayList<>();
    private int fileNum = 3;

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
        bucketName = enableVerBucketName;
        s3Client = S3Utils.buildS3Client();
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, objectName );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // create multiple versions object in the bucket
        for ( int i = 0; i < fileNum; i++ ) {
            objectVSList
                    .add( s3Client.putObject( new PutObjectRequest( bucketName,
                            objectName, new File( filePathList.get( i ) ) ) ) );
        }

        // get history eTag
        String histETag1 = objectVSList.get( fileNum - 3 ).getETag();
        String histEtag2 = objectVSList.get( fileNum - 2 ).getETag();

        // get histroy version
        String versionId1 = objectVSList.get( fileNum - 3 ).getVersionId();
        String versionId2 = objectVSList.get( fileNum - 2 ).getVersionId();
        String versionid3 = objectVSList.get( fileNum - 1 ).getVersionId();

        // get the lastModified of the version
        Date modified = getLastModified( bucketName, objectName, versionId1 );
        Date unModified = getLastModified( bucketName, objectName, versionid3 );

        // get object by
        // matchingETag/nonMatchingETag/modifiedSince/unModifiedSince
        S3Object currObject = s3Client.getObject(
                new GetObjectRequest( bucketName, objectName, versionId2 )
                        .withMatchingETagConstraint( histEtag2 )
                        .withNonmatchingETagConstraint( histETag1 )
                        .withModifiedSinceConstraint( modified )
                        .withUnmodifiedSinceConstraint( unModified ) );

        // check the eTag and the content of object
        String currPath = filePathList.get( fileNum - 2 );
        chectResult( currObject, currPath );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        objectName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void chectResult( S3Object object, String filePath )
            throws Exception {
        Assert.assertEquals( object.getObjectMetadata().getETag(),
                TestTools.getMD5( filePath ) );
        S3ObjectInputStream s3InputStream = null;
        try {
            s3InputStream = object.getObjectContent();
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            S3Utils.inputStream2File( s3InputStream, downloadPath );
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( filePath ) );
        } finally {
            if ( s3InputStream != null ) {
                s3InputStream.close();
            }
        }
    }

    private Date getLastModified( String bucketName, String objectName,
            String versionId ) {
        GetObjectRequest getObjectRequest = new GetObjectRequest( bucketName,
                objectName, versionId );
        S3Object object = s3Client.getObject( getObjectRequest );
        return object.getObjectMetadata().getLastModified();
    }
}
