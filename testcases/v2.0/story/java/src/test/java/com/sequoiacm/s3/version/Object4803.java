package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4803 :: S3接口创建多版本文件，SCM API不指定版本删除版本文件
 * @author wuyan
 * @Date 2022.07.18
 * @version 1.00
 */
public class Object4803 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4803";
    private String keyName = "object4803";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024 * 3;
    private int updateSize = 1024;
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

        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );
    }

    @Test
    public void test() throws Exception {
        ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                bucketName );
        scmBucket.deleteFile( keyName, false );
        // 获取当前版本文件不存在为删除标记
        try {
            scmBucket.getFile( keyName );
            Assert.fail( "get file with deleteMarker should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "FILE_NOT_FOUND",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 检查删除文件移到历史版本，版本号为2
        int historyVersion1 = 2;
        ScmFile file1 = scmBucket.getFile( keyName, historyVersion1, 0 );
        S3Utils.checkFileContent( file1, updatePath, localPath );

        // 检查原历史版本文件，版本号为1
        int historyVersion2 = 1;
        ScmFile file2 = scmBucket.getFile( keyName, historyVersion2, 0 );
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
}
