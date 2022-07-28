package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @description SCM-4340:上传多个分段不存在partNum为1、不连续分段号且分段长度不一致
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4340 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4340";
    private String keyName = "key4340";
    private AmazonS3 s3Client = null;
    private long m = 1024 * 1024;
    private long fileSize = 45 * m;
    private File localPath = null;
    private File file = null;
    private String filePath = null;
    private String uploadId = "";
    private List< PartETag > partEtags = new CopyOnWriteArrayList<>();
    private List< long[] > partList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );

        // partList{part1[filepositon, partSize, partNumber],part2[filepositon,
        // partSize, partNumber],...,partN[filepositon, partSize, partNumber]}
        long[] parts = new long[] { 0, 10 * m, 1 };
        partList.add( parts );
        parts = new long[] { 10 * m, 5 * m, 3 };
        partList.add( parts );
        parts = new long[] { 15 * m, 10 * m, 5 };
        partList.add( parts );
        parts = new long[] { 25 * m, 20 * m, 60 };
        partList.add( parts );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testUpload() throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        ThreadExecutor es = new ThreadExecutor();
        for ( int i = 0; i < partList.size(); i++ ) {
            es.addWorker( new ThreadUploadPart4340( partList.get( i ) ) );
        }
        es.run();

        // 完成分段上传
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );
        String expMd5 = TestTools.getMD5( filePath );
        String actMd5 = S3Utils.getMd5OfObject( s3Client, localPath, bucketName,
                keyName );
        Assert.assertEquals( actMd5, expMd5 );
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
            s3Client.shutdown();
        }
    }

    class ThreadUploadPart4340 {
        private AmazonS3 s3Client = S3Utils.buildS3Client();
        private long filepositon;
        private long partSize;
        private int partNumber;

        public ThreadUploadPart4340( long[] parts ) throws Exception {
            this.filepositon = parts[ 0 ];
            this.partSize = parts[ 1 ];
            this.partNumber = ( int ) parts[ 2 ];
        }

        @ExecuteOrder(step = 1, desc = "分段上传对象")
        public void putObject() {
            try {
                UploadPartRequest partRequest = new UploadPartRequest()
                        .withFile( file ).withFileOffset( filepositon )
                        .withPartNumber( partNumber ).withPartSize( partSize )
                        .withBucketName( bucketName ).withKey( keyName )
                        .withUploadId( uploadId );
                UploadPartResult uploadPartResult = s3Client
                        .uploadPart( partRequest );
                partEtags.add( uploadPartResult.getPartETag() );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
