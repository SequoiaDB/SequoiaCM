package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4748 :: 非桶管理用户增加文件
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4748 extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4748";
    private String key = "/aa/bb/object4748";
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsA = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private String userName = "user4748";
    private String passWord = "passwd4748";

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        SiteWrapper site = ScmInfo.getSite();
        sessionA = ScmSessionUtils.createSession( site );
        S3Utils.clearBucket( sessionA, bucketName );
        ScmAuthUtils.createUser( sessionA, userName, passWord );
        sessionB = ScmSessionUtils.createSession( ScmInfo.getSite(), userName,
                passWord );
        wsA = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, sessionA );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( wsA, bucketName );
        bucket.enableVersionControl();
    }

    @Test
    public void test() throws Exception {
        createScmFile( sessionA, filePath );

        try {
            createScmFile( sessionB, filePath );
            Assert.fail( "create file with other user should failed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError().getErrorCode(),
                    ScmError.OPERATION_UNAUTHORIZED.getErrorCode() );
            Assert.assertEquals( e.getError().getErrorDescription(),
                    "Unauthorized operation" );
        }

        ScmBucket bucket = ScmFactory.Bucket.getBucket( sessionA, bucketName );
        ScmFile file = bucket.getFile( key );
        Assert.assertEquals( file.getMajorVersion(), 1 );
        Assert.assertEquals( file.getSize(), fileSize );
        S3Utils.checkFileContent( file, filePath, localPath );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( sessionA, s3WorkSpaces, bucketName );
                ScmFactory.User.deleteUser( sessionA, userName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void createScmFile( ScmSession session, String path )
            throws ScmException {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        ScmFileUtils.createFile( bucket, key, path );
    }

}
