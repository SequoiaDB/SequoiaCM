package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Description SCM-4802 :: S3接口创建多版本文件，SCM API获取版本文件
 * @author wuyan
 * @Date 2022.07.14
 * @version 1.00
 */
public class Object4802 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4802";
    private String keyName = "object4802";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024 * 3;
    private int updateSize = 1024;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private ScmSession session;
    private SiteWrapper site = null;
    private ScmId fileId = null;

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

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        S3Utils.clearBucket( session, bucketName );

        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );
    }

    @Test
    public void testCreateObject() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                bucketName );
        long bucketId = scmBucket.getId();

        // testa：不指定版本获取文件，检查属性和内容
        ScmFile file = scmBucket.getFile( keyName );
        fileId = file.getFileId();
        checkFileAttributes( file, currentVersion, updateSize, bucketId,
                updatePath );
        S3Utils.checkFileContent( file, updatePath, localPath );

        // testb：指定最新版本获取文件，检查属性和内容
        ScmFile file1 = scmBucket.getFile( keyName, currentVersion, 0 );
        checkFileAttributes( file1, currentVersion, updateSize, bucketId,
                updatePath );
        S3Utils.checkFileContent( file1, updatePath, localPath );

        // testc：指定历史版本获取文件，检查属性和内容
        ScmFile file2 = scmBucket.getFile( keyName, historyVersion, 0 );
        checkFileAttributes( file2, historyVersion, fileSize, bucketId,
                filePath );
        S3Utils.checkFileContent( file2, filePath, localPath );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileAttributes( ScmFile file, int fileVersion,
            long fileSize, long bucketId, String path ) throws IOException {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), keyName );
        Assert.assertEquals( file.getTitle(), "" );
        Assert.assertEquals( file.getBucketId().longValue(), bucketId );
        Assert.assertEquals( file.getSize(), fileSize );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), fileVersion );
        Assert.assertFalse( file.isNullVersion() );
        Assert.assertEquals( file.getMd5(), TestTools.getMD5AsBase64( path ) );
    }
}
