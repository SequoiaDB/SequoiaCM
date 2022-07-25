package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @descreption SCM-4221 :: SCM API创建S3文件，S3接口更新文件
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4221 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4221";
    private String key = "aa/bb/object4221";
    private File localPath = null;
    private String updatePath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
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

        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws Exception {
        // scm create bucket
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        S3Utils.updateBucketVersionConfig( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        // scm create file, with x-amz-meta
        ScmFile file = createScmFile();
        // scm attach file to bucket
        ScmFactory.Bucket.attachFile( session, bucketName, file.getFileId() );
        // s3 put object. object name is same with file path
        updateObject();
        // scm get file , check file content and x-amz-meta
        checkFile( bucket, key, updatePath );

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

    private ScmFile createScmFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        try {
            file.setContent( filePath );
            file.setFileName( key );
            file.setAuthor( "author4221" );

            Map< String, String > customMeta = new HashMap<>();
            customMeta.put( user_meta_key, user_meta_value1 );
            file.setCustomMetadata( customMeta );
            file.save( new ScmUploadConf( true, true ) );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        return file;
    }

    private void updateObject() {
        Map< String, String > xMeta = new HashMap<>();
        xMeta.put( user_meta_key, user_meta_value2 );
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setUserMetadata( xMeta );
        PutObjectRequest request = new PutObjectRequest( bucketName, key,
                new File( updatePath ) );
        request.setMetadata( metaData );

        s3Client.putObject( request );
    }

    private void checkFile( ScmBucket bucket, String objectName,
            String path ) throws Exception {
        ScmFile file = bucket.getFile( objectName );
        file.getMd5();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileOutputStream = new FileOutputStream( downloadPath );
        file.getContent( fileOutputStream );
        fileOutputStream.close();

        String updateMD5 = TestTools.getMD5( path );
        String downloadMD5 = TestTools.getMD5( downloadPath );
        Assert.assertEquals( updateMD5, downloadMD5 );
        Assert.assertEquals( file.getCustomMetadata().get( user_meta_key ),
                user_meta_value2 );
    }
}
