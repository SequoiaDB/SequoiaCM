package com.sequoiacm.s3.object.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3386:并发增加和删除相同对象
 * @author fanyu
 * @Date 2019.01.03
 * @version 1.00
 */
public class UpdateAndDeleteObject3386 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3386";
    private String keyNameBase = "key3386";
    private List< String > keyNameList = new ArrayList<>();
    private int objectNum = 20;
    private String content = "testContent3386";
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

    // TODO: 受SEQUOIACM-1007影响暂时屏蔽
    @Test(enabled = false)
    public void testCreateAndDeleteObject() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( String keyName : keyNameList ) {
            threadExec.addWorker( new UpdateObject( keyName ) );
            threadExec.addWorker( new DeleteObject( keyName ) );
        }
        threadExec.run();

        // check result
        ObjectListing objectList = s3Client.listObjects( bucketName );
        List< S3ObjectSummary > objects = objectList.getObjectSummaries();
        for ( S3ObjectSummary obj : objects ) {
            Assert.assertTrue( obj.getKey().startsWith( "aa" ),
                    "key = " + obj.getKey() );
            Assert.assertTrue( obj.getKey().contains( "aa" ),
                    "key = " + obj.getKey() );
        }
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

    private class UpdateObject {
        private String keyName;

        private UpdateObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            AmazonS3 s3Client = null;
            try {
                s3Client = S3Utils.buildS3Client();
                s3Client.putObject( bucketName, keyName, content );
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
