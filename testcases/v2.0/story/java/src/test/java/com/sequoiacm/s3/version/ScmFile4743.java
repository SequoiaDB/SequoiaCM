package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @descreption SCM-4743 :: 更新桶状态为禁用（enable->suspended），更新同名文件
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4743 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4743";
    private String key = "/aa/bb/object4743";
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;

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
        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        ScmFactory.Bucket.createBucket( ws, bucketName );
    }

    @Test
    public void test() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );

        bucket.enableVersionControl();

        ScmFileUtils.createFile( bucket, key, filePath );

        bucket.suspendVersionControl();

        ScmFileUtils.createFile( bucket, key, updatePath );

        checkFile();

        checkFileList();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
                TestTools.LocalFile.removeFile( localPath );
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
        Assert.assertEquals( scmFile.getMd5(),
                TestTools.getMD5AsBase64( updatePath ) );
        Assert.assertEquals( scmFile.getSize(), updateSize );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileOutputStream = new FileOutputStream( downloadPath );
        scmFile.getContent( fileOutputStream );
        fileOutputStream.close();

        String downloadMD5 = TestTools.getMD5( downloadPath );
        String fileMD5 = TestTools.getMD5( updatePath );
        Assert.assertEquals( fileMD5, downloadMD5 );
    }

    private void checkFileList() throws Exception {
        List< ScmFileBasicInfo > fileList = S3Utils.getVersionList( session, ws,
                bucketName );

        Assert.assertFalse( fileList.get( 0 ).isDeleteMarker() );
        Assert.assertTrue( fileList.get( 0 ).isNullVersion() );
        Assert.assertEquals(
                fileList.get( 0 ).getVersionSerial().getMajorSerial(), 2 );

        Assert.assertFalse( fileList.get( 1 ).isDeleteMarker() );
        Assert.assertFalse( fileList.get( 1 ).isNullVersion() );
        Assert.assertEquals( fileList.get( 1 ).getMajorVersion(), 1 );
    }
}
