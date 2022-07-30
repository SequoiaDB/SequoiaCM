package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @descreption SCM-4747 :: 桶开启版本控制，增加多个同名文件
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4747 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4747";
    private String key = "/aa/bb/object4747";
    private File localPath = null;
    private int fileSize = 1024;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
    }

    @Test
    public void test() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );

        for ( int i = 0; i < 200; i++ ) {
            String filePath = localPath + File.separator + "localFile_" + i
                    + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize );
            S3Utils.createFile(bucket, key, filePath);
        }

        checkFile();

        checkFileList();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFile() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        ScmFile scmFile = bucket.getFile( key );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileOutputStream = new FileOutputStream( downloadPath );
        scmFile.getContent( fileOutputStream );
        fileOutputStream.close();

        String downloadMD5 = TestTools.getMD5( downloadPath );
        String filePath = localPath + File.separator + "localFile_" + 199
                + ".txt";
        String fileMD5 = TestTools.getMD5( filePath );
        Assert.assertEquals( fileMD5, downloadMD5 );
    }

    private void checkFileList() throws Exception {
        int count = 200;
        List< ScmFileBasicInfo > fileList = S3Utils.getVersionList(session, ws, bucketName);
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );

        Assert.assertEquals( fileList.size(), 200 );
        for ( ScmFileBasicInfo file : fileList ) {
            Assert.assertEquals( file.getMajorVersion(), count );
            Assert.assertFalse( file.isDeleteMarker() );
            Assert.assertFalse( file.isNullVersion() );

            ScmFile scmFile = bucket.getFile( file.getFileName(),
                    file.getMajorVersion(), file.getMinorVersion() );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            OutputStream fileOutputStream = new FileOutputStream(
                    downloadPath );
            scmFile.getContent( fileOutputStream );
            fileOutputStream.close();

            String downloadMD5 = TestTools.getMD5( downloadPath );
            String filePath = localPath + File.separator + "localFile_"
                    + ( count - 1 ) + ".txt";
            String fileMD5 = TestTools.getMD5( filePath );
            Assert.assertEquals( fileMD5, downloadMD5 );

            count--;
        }
        Assert.assertEquals( count, 0 );
    }
}
