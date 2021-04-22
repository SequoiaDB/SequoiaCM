package com.sequoiacm.s3.concurrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.Md5Utils;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3387:并发增加和删除相同对象
 * @author wangkexin
 * @Date 2019.01.03
 * @version 1.00
 */
public class GetAndDeleteObject3387 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3387";
    private String keyNameBase = "key3387";
    private List< String > keyNameList = new ArrayList<>();
    private int objectNum = 20;
    private String content = "testContent3387";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        for ( int i = 0; i < objectNum; i++ ) {
            s3Client.putObject( bucketName, "aa" + i + "/" + keyNameBase,
                    content );
            keyNameList.add( "aa" + i + "/" + keyNameBase );
        }
    }

    @Test
    public void testCreateAndDeleteObject() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( String keyName : keyNameList ) {
            threadExec.addWorker( new GetObject( keyName ) );
            threadExec.addWorker( new DeleteObject( keyName ) );
        }
        threadExec.run();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                for ( String keyName : keyNameList ) {
                    s3Client.deleteObject( bucketName, keyName );
                }
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private class GetObject {
        private String keyName;

        private GetObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws IOException {
            AmazonS3 s3Client = null;
            try {
                s3Client = S3Utils.buildS3Client();
                S3Object s3Object = s3Client.getObject( bucketName, keyName );
                ObjectMetadata metadata = s3Object.getObjectMetadata();
                String etag = metadata.getETag();
                Assert.assertEquals( etag,
                        TestTools.getMD5( content.getBytes() ) );
                Assert.assertEquals(
                        Md5Utils.md5AsBase64( s3Object.getObjectContent() ),
                        Md5Utils.md5AsBase64( content.getBytes() ) );
            } catch ( AmazonS3Exception e ) {
                if ( e.getStatusCode() != 404 ) {
                    throw e;
                }
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class DeleteObject {
        private String keyName;

        public DeleteObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = null;
            try {
                s3Client = S3Utils.buildS3Client();
                s3Client.deleteObject( bucketName, keyName );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
