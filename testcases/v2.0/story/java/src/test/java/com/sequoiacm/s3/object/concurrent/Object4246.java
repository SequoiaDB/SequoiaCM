package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @descreption SCM-4246 :: S3接口更新文件，SCM接口并发更新文件自由标签
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4246 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4246";
    private String key = "aa/bb/object4246";
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

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );

        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, key, "aaa" );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 10000 );
        S3UpdateObject t1 = new S3UpdateObject();
        ScmUpdateFile t2 = new ScmUpdateFile();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        S3Object s3Object = s3Client.getObject( bucketName, key );
        String userMeta = s3Object.getObjectMetadata().getUserMetadata()
                .get( user_meta_key );
        if ( !( user_meta_value1.equals( userMeta ) )
                && !( user_meta_value2.equals( userMeta ) ) ) {
            Assert.fail( "update failed" );
        }

        String fileMd5 = TestTools.getMD5( filePath );
        String objectMd5 = TestTools
                .getMD5( s3Object.getObjectContent() );
        Assert.assertEquals( objectMd5, fileMd5 );

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

    class S3UpdateObject {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                Thread.sleep( 10 );
                Map< String, String > xMeta = new HashMap<>();
                xMeta.put( user_meta_key, user_meta_value1 );
                ObjectMetadata metaData = new ObjectMetadata();
                metaData.setUserMetadata( xMeta );
                PutObjectRequest request = new PutObjectRequest( bucketName,
                        key, new File( filePath ) );
                request.setMetadata( metaData );

                s3Client.putObject( request );
            } catch ( AmazonS3Exception e ) {
                Assert.fail( "s3 put object failed" , e);
            }
        }
    }

    class ScmUpdateFile {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = bucket.getFile( key );
                Thread.sleep( 10 );

                Map< String, String > customMeta = new HashMap<>();
                customMeta.put( user_meta_key, user_meta_value2 );
                file.setCustomMetadata( customMeta );
            } catch ( ScmException e ) {
                //TODO:SEQUOIACM-876修改后，根据新的错误描述修改判断值
                Assert.assertEquals( e.getError().getErrorDescription(), "File not found" );
            }
        }
    }
}
