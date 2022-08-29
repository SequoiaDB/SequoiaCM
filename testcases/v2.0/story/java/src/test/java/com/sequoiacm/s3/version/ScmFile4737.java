package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @descreption SCM-4737 :: 开启版本控制，增加一个文件
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4737 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4737";
    private String key = "object4737";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private Date createTime = null;
    private Date dataTime = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );

        createScmFile( bucket, filePath );
        checkFile( bucket, key, filePath );

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

    private void createScmFile( ScmBucket bucket, String path )
            throws ScmException {
        ScmFile file = bucket.createFile( key );
        file.setContent( path );
        file.setAuthor( "author4737" );

        file.save();
        createTime = file.getCreateTime();
        dataTime = file.getDataCreateTime();
    }

    private void checkFile( ScmBucket bucket, String objectName, String path )
            throws Exception {
        ScmFile file = bucket.getFile( objectName );
        String md5 = file.getMd5();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileOutputStream = new FileOutputStream( downloadPath );
        file.getContent( fileOutputStream );
        fileOutputStream.close();

        String updateMD5 = TestTools.getMD5( path );
        String downloadMD5 = TestTools.getMD5( downloadPath );
        Assert.assertEquals( updateMD5, downloadMD5 );

        Assert.assertEquals( file.getMajorVersion(), 1);
        Assert.assertEquals( file.getSize(), fileSize);
        Assert.assertEquals( file.getCreateTime(), createTime);
        Assert.assertEquals( file.getDataCreateTime(), dataTime);
    }
}
