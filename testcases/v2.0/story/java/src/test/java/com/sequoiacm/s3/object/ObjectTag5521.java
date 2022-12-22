package com.sequoiacm.s3.object;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5521:S3 API上传到对象时，设置标签
 * @author YiPan
 * @date 2022/12/8
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5521 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private List< Tag > tagSet = new ArrayList<>();
    private Map< String, String > map = new HashMap<>();
    private String bucketName;
    private String keyName = "Object5521";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        bucketName = TestScmBase.bucketName;

        s3Client = S3Utils.buildS3Client();
        initTag();
    }

    @Test
    public void test() throws ScmException {
        // 普通上传设置标签
        PutObjectRequest request = new PutObjectRequest( bucketName, keyName,
                filePath );
        request.withTagging( new ObjectTagging( tagSet ) );
        s3Client.putObject( request );

        // 校验标签
        GetObjectTaggingResult tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        // TODO: SEQUOIACM-1176
        // S3Utils.compareTagSet( tagging.getTagSet(), tagSet );

        // 删除文件
        s3Client.deleteObject( bucketName, keyName );

        // 分段上传
        String uploadId = initPartUpload( s3Client, bucketName, keyName,
                tagSet );
        List< PartETag > partEtags = PartUploadUtils.partUpload( s3Client,
                bucketName, keyName, uploadId, new File( filePath ) );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );

        // 校验标签
        tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        // TODO: SEQUOIACM-1176
        // S3Utils.compareTagSet( tagging.getTagSet(), tagSet );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                s3Client.deleteObject( bucketName, keyName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void initTag() {
        String baseKey = "test";
        String baseValue = "value";
        for ( int i = 0; i < 9; i++ ) {
            tagSet.add( new Tag( baseKey + i, baseValue + i ) );
            map.put( baseKey + i, baseValue + i );
        }
    }

    public static String initPartUpload( AmazonS3 s3Client, String bucketName,
            String key, List< Tag > tagSet ) {
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
                bucketName, key );
        initRequest.setTagging( new ObjectTagging( tagSet ) );
        ObjectMetadata metadata = new ObjectMetadata();
        initRequest.setObjectMetadata( metadata );
        InitiateMultipartUploadResult result = s3Client
                .initiateMultipartUpload( initRequest );
        String uploadId = result.getUploadId();
        return uploadId;
    }
}
