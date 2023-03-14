package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Description SCM-4794 :: SCM创建版本文件，S3更新版本文件
 * @author wuyan
 * @Date 2022.07.14
 * @version 1.00
 */
public class Object4794 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4794";
    private String keyName = "object4794";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private ScmSession session;
    private SiteWrapper site = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
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
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        fileId = ScmFileUtils.createFile( scmBucket, keyName, filePath );
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );

        // 获取当前版本信息检查属性和内容
        int currentVersion = 2;
        ScmFile file = scmBucket.getFile( keyName );
        checkFileAttributes( file, currentVersion, updateSize, updatePath );
        S3Utils.checkFileContent( file, updatePath, localPath );

        // 获取历史版本属性信息和内容
        int historyVersion = 1;
        ScmFile file1 = scmBucket.getFile( keyName, historyVersion, 0 );
        checkFileAttributes( file1, historyVersion, fileSize, filePath );
        S3Utils.checkFileContent( file1, filePath, localPath );
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

    private void checkFileAttributes( ScmFile file, int version, long size,
            String path ) throws IOException {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), keyName );
        Assert.assertEquals( file.getSize(), size );
        Assert.assertEquals( file.getBucketId().longValue(),
                scmBucket.getId() );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), version );
        Assert.assertFalse( file.isNullVersion() );
        Assert.assertEquals( file.getMd5(), TestTools.getMD5AsBase64( path ) );
    }
}
