package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
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
 * @descreption SCM-4217 :: SCM API创建S3文件，S3接口更新文件
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4217 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4217";
    private String key = "aa/bb/object4217";
    private File localPath = null;
    private String updatePath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
    private String customMetaKey = "test";
    private String customMetaValue = "aaa";

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
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws Exception {
        // scm create bucket
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );

        // scm create file(content 1), specify x-amz-meta
        createScmFile( bucket );

        // s3 put same object , specify x-amz-meta, different content(file) with file(content 1)
        updateObject();

        // scm get file ?, check x-amz-meta and content md5
        checkFileMetaAndContent( bucket );

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
        try {
            file.setContent( filePath );
            file.setFileName( key );
            file.setAuthor( "author4217" );

            file.save( new ScmUploadConf( true, true ) );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkFileMetaAndContent( ScmBucket bucket )
            throws Exception {
        // scm get file, check content md5
        ScmFile file = bucket.getFile( key );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileOutputStream = new FileOutputStream( downloadPath );
        file.getContent( fileOutputStream );
        fileOutputStream.close();

        Assert.assertEquals( file.getCustomMetadata().get( customMetaKey ),
                customMetaValue );
        Assert.assertEquals( TestTools.getMD5( updatePath ),
                TestTools.getMD5( downloadPath ) );
    }

    private void updateObject() {
        Map< String, String > xMeta = new HashMap< String, String >();
        xMeta.put( customMetaKey, customMetaValue );
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setUserMetadata( xMeta );
        PutObjectRequest request = new PutObjectRequest( bucketName, key,
                new File( updatePath ) );
        request.setMetadata( metaData );

        s3Client.putObject( request );
    }
}
