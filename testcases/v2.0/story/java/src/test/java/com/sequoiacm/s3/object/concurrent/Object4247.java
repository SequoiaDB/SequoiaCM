package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
 * @descreption SCM-4247 :: S3接口更新自由标签，SCM API并发删除文件
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4247 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4247";
    private String key = "aa/bb/object4247";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private String user_meta_key = "test";
    private String user_meta_value = "aaa";

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
        session = TestScmTools.createSession( site );

        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, key, new File( filePath ) );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 10000 );
        S3UpdateObject t1 = new S3UpdateObject();
        ScmDeleteFile t2 = new ScmDeleteFile();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        s3Client.putObject( bucketName, key, new File(
                filePath ) );

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
            Thread.sleep( 100 );
            Map< String, String > xMeta = new HashMap<>();
            xMeta.put( user_meta_key, user_meta_value );
            ObjectMetadata metaData = new ObjectMetadata();
            metaData.setUserMetadata( xMeta );
            PutObjectRequest request = new PutObjectRequest( bucketName,
                    key, new File( filePath ) );

            s3Client.putObject( request );
        }
    }

    class ScmDeleteFile {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            ScmFile file = bucket.getFile( key );
            System.out.println("file1 version:" + file.getMajorVersion() + "." + file.getMinorVersion());
            Thread.sleep( 200 );
            file.delete( true );
        }
    }
}
