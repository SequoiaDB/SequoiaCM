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
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @descreption SCM-4746 :: 更新桶状态为禁用（disable->suspended），增加同名文件
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4746 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4746";
    private String key = "/aa/bb/object4746";
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
    private String user_meta_key = "test";
    private String[] user_meta_value = { "aaaa", "bbbb" };

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

        createScmFile( bucket, filePath, user_meta_value[ 0 ] );

        bucket.suspendVersionControl();

        createScmFile( bucket, updatePath, user_meta_value[ 1 ] );

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

    private void createScmFile( ScmBucket bucket, String path, String meta )
            throws ScmException, IOException {
        ScmFile file = bucket.createFile( key );
        file.setContent( path );
        file.setAuthor( "author4746" );
        Map< String, String > customMeta = new HashMap<>();
        customMeta.put( user_meta_key, meta );
        file.setCustomMetadata( customMeta );
        file.save();

        file = bucket.getFile( key );
        String md5 = file.getMd5();
        String fileMD5 = TestTools.getMD5AsBase64( path );
        Assert.assertEquals( md5, fileMD5 );
        Assert.assertEquals( file.getCustomMetadata().get( user_meta_key ),
                meta );
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
        String fileMD5 = TestTools.getMD5( updatePath );
        Assert.assertEquals( fileMD5, downloadMD5 );
        Assert.assertEquals( scmFile.getCustomMetadata().get( user_meta_key ),
                user_meta_value[ 1 ] );
    }

    private void checkFileList() throws Exception {
        List< ScmFileBasicInfo > fileList = S3Utils.getVersionList( session, ws,
                bucketName );

        Assert.assertEquals( fileList.size(), 1 );
        Assert.assertFalse( fileList.get( 0 ).isDeleteMarker() );
        Assert.assertTrue( fileList.get( 0 ).isNullVersion() );
        Assert.assertEquals(
                fileList.get( 0 ).getVersionSerial().getMajorSerial(), 2 );
    }
}
