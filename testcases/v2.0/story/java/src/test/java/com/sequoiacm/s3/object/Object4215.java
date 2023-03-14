package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @descreption SCM-4215 :: SCM API创建S3文件，S3接口查询文件
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4215 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4215";
    private String key = "aa/bb/object4215";
    private String contentType_key = "Content-Type";
    private String contentType_value = "binary/octet-stream";
    private String meta_test_key = "test";
    private String meta_test_value = "aaab";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException, IOException {
        // scm create bucket
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        // scm create file
        createScmFile( bucket );
        // s3 get object
        S3Object s3Object = s3Client.getObject( bucketName, key );
        // check file meta and data md5
        Assert.assertEquals( s3Object.getObjectMetadata().getETag(),
                TestTools.getMD5( filePath ) );
        Assert.assertEquals( s3Object.getObjectMetadata().getContentType(),
                contentType_value );
        Assert.assertEquals( s3Object.getObjectMetadata().getUserMetadata()
                .get( meta_test_key ), meta_test_value );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
            TestTools.LocalFile.removeFile( localPath );
        }

        if ( s3Client != null ) {
            s3Client.shutdown();
        }

        if ( session != null ) {
            session.close();
        }
    }

    private void createScmFile( ScmBucket bucket )
            throws ScmException {
        ScmFile file = bucket.createFile( key );

        file.setContent( filePath );
        file.setFileName( key );
        file.setAuthor( "author4215" );
        file.setMimeType( contentType_value );

        Map< String, String > customMeta = new HashMap<>();
        customMeta.put( meta_test_key, meta_test_value );
        file.setCustomMetadata( customMeta );
        file.save();
    }
}
