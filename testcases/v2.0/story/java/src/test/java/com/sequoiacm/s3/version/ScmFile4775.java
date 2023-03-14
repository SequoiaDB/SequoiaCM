package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4775 :: 禁用版本控制，带版本号删除最新版本文件
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4775 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4775";
    private String fileName = "scmfile4775";
    private SiteWrapper site = null;
    private int fileSize = 1024 * 300;
    private int updateSize = 1024 * 128;
    private String filePath = null;
    private String updatePath = null;
    private File localPath = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

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
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        ScmFileUtils.createFile( scmBucket, fileName, filePath );
        ScmFileUtils.createFile( scmBucket, fileName, updatePath );
        ScmFileUtils.createFile( scmBucket, fileName, filePath );
        scmBucket.suspendVersionControl();
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        int currentVersion = 3;
        scmBucket.deleteFileVersion( fileName, currentVersion, 0 );
        try {
            scmBucket.getFile( fileName, currentVersion, 0 );
            Assert.fail( "get file with currentVersion should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 获取当前版本文件为原v2版本文件
        ScmFile file = scmBucket.getFile( fileName );
        Assert.assertEquals( file.getSize(), updateSize );
        Assert.assertEquals( file.getMajorVersion(), 2 );
        S3Utils.checkFileContent( file, updatePath, localPath );
        // 获取原v1版本文件未删除
        int newhistroyVersion = 1;
        ScmFile file1 = scmBucket.getFile( fileName, newhistroyVersion, 0 );
        Assert.assertEquals( file1.getSize(), fileSize );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
