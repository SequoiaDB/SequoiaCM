package com.sequoiacm.s3.version;

import java.io.File;
import java.util.List;

import com.sequoiacm.client.element.ScmFileBasicInfo;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-5041 :: SCM删除版本，S3更新版本文件
 * @author Zhaoyujing
 * @Date 2020/08/01
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object5041 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket5041";
    private String keyName = "object5041";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private ScmSession session;
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

        session = TestScmTools.createSession( ScmInfo.getSite() );
        S3Utils.clearBucket( session, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        s3Client = S3Utils.buildS3Client();
        fileId = S3Utils.createFile( scmBucket, keyName, filePath );
    }

    @Test
    public void testCreateObject() throws Exception {
        ScmFile file = scmBucket.getFile( keyName );
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );
        file.deleteVersion();

        int currentVersion = 2;
        file = scmBucket.getFile( keyName );
        checkFileAttributes( file, currentVersion, updateSize );
        S3Utils.checkFileContent( file, updatePath, localPath );

        List< ScmFileBasicInfo > versionList = S3Utils.getVersionList( session,
                ws, bucketName );
        Assert.assertEquals( versionList.size(), 1 );
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

    private void checkFileAttributes( ScmFile file, int version, long size ) {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), keyName );
        Assert.assertEquals( file.getSize(), size );
        Assert.assertEquals( file.getBucketId().longValue(),
                scmBucket.getId() );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(),
                version );
        Assert.assertFalse( file.isNullVersion() );
    }
}
