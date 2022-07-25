package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @descreption SCM-4226 S3接口创建S3文件，SCM API更新文件指定不计算MD5
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4226 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4226";
    private String key = "aa/bb/object4226";
    private File localPath = null;
    private String updatePath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
    private String contentType_value1 = "text/plain";
    private String contentType_value2 = "binary/octet-stream";
    private String user_meta_key = "test";
    private String user_meta_value1 = "aaaa";
    private String user_meta_value2 = "bbbb";

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
        TestTools.LocalFile.createFile( updatePath, updateSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
    }

    @Test
    public void test() throws ScmException, IOException {
        // s3 create bucket
        s3Client.createBucket( bucketName );
        // s3 put object , specify x-amz-meta
        putObject( filePath );
        // scm update file,
        updateScmFile( updatePath );
        // s3 get object, check object meta and content md5
        checkObject();

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
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void putObject( String filePath ) {
        Map< String, String > xMeta = new HashMap<>();
        xMeta.put( user_meta_key, user_meta_value1 );
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setUserMetadata( xMeta );
        metaData.setContentType( contentType_value1 );
        PutObjectRequest request = new PutObjectRequest( bucketName, key,
                new File( filePath ) );
        request.setMetadata( metaData );

        s3Client.putObject( request );
    }

    private void updateScmFile( String filePath )
            throws ScmException, IOException {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        ScmFile file = bucket.getFile( key );

        // update content
        file.updateContent( new FileInputStream( filePath ),
                new ScmUpdateContentOption( false ) );

        // update meta
        file.setMimeType( contentType_value2 );
        Map< String, String > customMeta = new HashMap<>();
        customMeta.put( user_meta_key, user_meta_value2 );
        file.setCustomMetadata( customMeta );
    }

    private void checkObject() throws IOException {
        S3Object updateObject = s3Client.getObject( bucketName, key );
        ObjectMetadata updateObjectMeta = updateObject.getObjectMetadata();

        String objectMd5 = TestTools.getMD5( updateObject.getObjectContent() );
        String fileMd5 = TestTools.getMD5( updatePath );

        Assert.assertEquals( updateObjectMeta.getETag(), fileMd5 );
        Assert.assertEquals( objectMd5, fileMd5 );
        Assert.assertEquals( updateObjectMeta.getContentType(),
                contentType_value2 );
        Assert.assertEquals(
                updateObjectMeta.getUserMetadata().get( user_meta_key ),
                user_meta_value2 );
    }
}
