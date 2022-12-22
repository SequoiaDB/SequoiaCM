package com.sequoiacm.s3.object;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5523:S3复制对象时操作标签
 * @author YiPan
 * @date 2022/12/8
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5523 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private List< Tag > tagSet = new ArrayList<>();
    private List< Tag > newTagSet = new ArrayList<>();
    private String bucketName;
    private String srcKeyName = "Object5523a";
    private String destKeyName = "Object5523b";
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
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, srcKeyName );
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, destKeyName );
        initTag();
    }

    @Test
    public void test() throws ScmException {
        // 上传文件
        s3Client.putObject( bucketName, srcKeyName, filePath );
        S3Utils.setObjectTag( s3Client, bucketName, srcKeyName, tagSet );

        // 复制文件及标签
        CopyObjectRequest request = new CopyObjectRequest( bucketName,
                srcKeyName, bucketName, destKeyName );
        request.setMetadataDirective( "REPLACE" );
        s3Client.copyObject( request );

        // 校验标签
        GetObjectTaggingResult tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, destKeyName ) );
        S3Utils.compareTagSet( tagging.getTagSet(), tagSet );

        // 删除复制后的对象
        s3Client.deleteObject( bucketName, destKeyName );

        // 复制文件，设置新标签
        request = new CopyObjectRequest( bucketName, srcKeyName, bucketName,
                destKeyName );
        request.setMetadataDirective( "REPLACE" );
        request.setNewObjectTagging( new ObjectTagging( newTagSet ) );
        s3Client.copyObject( request );

        // 校验标签
        tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, destKeyName ) );
        S3Utils.compareTagSet( tagging.getTagSet(), newTagSet );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        srcKeyName );
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        destKeyName );
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
        }
        newTagSet.add( new Tag( baseKey, baseValue ) );
    }
}
