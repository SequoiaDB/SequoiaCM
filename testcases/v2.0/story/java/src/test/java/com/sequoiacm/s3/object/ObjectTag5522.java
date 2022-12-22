package com.sequoiacm.s3.object;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.Tag;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5522:S3 API获取对象标签数量
 * @author YiPan
 * @date 2022/12/8
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5522 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private List< Tag > tagSet = new ArrayList<>();
    private String bucketName;
    private String keyName = "Object5522";
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
        // 上传文件
        s3Client.putObject( bucketName, keyName, filePath );

        // 获取标签数量
        GetObjectTaggingResult tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        int size = tagging.getTagSet().size();
        Assert.assertEquals( size, 0 );

        // 设置多个标签
        S3Utils.setObjectTag( s3Client, bucketName, keyName, tagSet );

        // 获取标签数量
        tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        size = tagging.getTagSet().size();
        Assert.assertEquals( size, tagSet.size() );
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
        }
    }
}
