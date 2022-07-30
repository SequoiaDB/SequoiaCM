package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
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
    private ScmSession sessionA;
    private ScmWorkspace ws = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private String wsName = "ws_4748";
    private String userName = "user4748";
    private String passWord = "passwd4748";
    private ScmSession sessionB = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        sessionA = TestScmTools.createSession( ScmInfo.getRootSite() );
        S3Utils.clearBucket( sessionA, s3WorkSpaces, bucketName );
        ScmWorkspaceUtil.deleteWs( wsName, sessionA );
        deleteUser( sessionA, userName );

        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, sessionA );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();

        ScmWorkspaceUtil.createS3WS( sessionA, wsName );
        ScmAuthUtils.createAdminUserGrant( sessionA, wsName, userName,
                passWord );
        sessionB = TestScmTools.createSession( ScmInfo.getRootSite(), userName,
                passWord );
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

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( sessionA, s3WorkSpaces, bucketName );
                ScmWorkspaceUtil.deleteWs( wsName, sessionA );
                ScmFactory.User.deleteUser( sessionA, userName );
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
        S3Utils.createFile(bucket, key, path);
    }

    private void deleteUser( ScmSession session, String userName ) {
        try {
            ScmFactory.User.deleteUser( session, userName );
        } catch ( ScmException e ) {

        }
    }
}
