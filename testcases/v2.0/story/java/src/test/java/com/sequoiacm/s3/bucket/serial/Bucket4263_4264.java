package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-4263:创建SCM文件，关联到有权限的桶 SCM-4264:创建SCM文件，关联到没有权限的桶
 * @Author YiPan
 * @CreateDate 2022/5/18
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4263_4264 extends TestScmBase {
    private final String bucketNameA = "bucket4263a";
    private final String bucketNameB = "bucket4263b";
    private final String objectKey = "object4263";
    private final String wsName = "ws4263";
    private ScmSession adminSession;
    private ScmSession newUserSession;
    private final String username = "user4263";
    private final String password = "passwd4263";
    private final String roleName = "ROLE_WS4263";
    private AmazonS3 s3Client = null;
    private String[] accessKeys = null;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        downloadPath = localPath + File.separator + "downLoadFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        adminSession = TestScmTools.createSession( ScmInfo.getRootSite() );

        // 新建ws，新建用户赋权
        ScmWorkspaceUtil.createS3WS( adminSession, wsName );
        ScmUser user = ScmAuthUtils.createUser( adminSession, username,
                password );
        newUserSession = TestScmTools.createSession( ScmInfo.getRootSite(),
                username, password );
        ScmRole role = ScmAuthUtils.createRole( adminSession, roleName );
        ScmAuthUtils.alterUser( adminSession, wsName, user, role,
                ScmPrivilegeType.ALL );
        ScmAuthUtils.checkPriorityByS3( newUserSession, wsName );

        // 创建新用户s3连接,确保s3节点也同步了权限
        accessKeys = ScmAuthUtils.refreshAccessKey( adminSession, username,
                password, null );
        s3Client = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );
        ScmAuthUtils.checkPriorityByS3( accessKeys, wsName );

        // 清理bucket
        S3Utils.clearBucket( adminSession, s3WorkSpaces, bucketNameA );
        S3Utils.clearBucket( newUserSession, wsName, bucketNameB );
    }

    @Test
    public void test() throws Exception {
        // 在s3 ws中创建桶A
        ScmWorkspace s3Ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces,
                adminSession );
        ScmFactory.Bucket.createBucket( s3Ws, bucketNameA );
        // 在新建ws中创建桶B
        ScmWorkspace newWs = ScmFactory.Workspace.getWorkspace( wsName,
                newUserSession );
        ScmFactory.Bucket.createBucket( newWs, bucketNameB );

        // 在新建ws中创建文件
        ScmFile file = ScmFactory.File.createInstance( newWs );
        file.setFileName( objectKey );
        file.setContent( filePath );
        ScmId fileId = file.save();

        // 挂载到没有权限的桶A中
        try {
            ScmFactory.Bucket.attachFile( newUserSession, bucketNameA, fileId );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.OPERATION_UNAUTHORIZED ) ) {
                throw e;
            }
        }

        // 挂载到有权限的桶B中
        ScmFactory.Bucket.attachFile( newUserSession, bucketNameB, fileId );
        S3Object object = s3Client.getObject( bucketNameB, objectKey );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( adminSession, s3WorkSpaces, bucketNameA );
                S3Utils.clearBucket( newUserSession, wsName, bucketNameB );
                ScmFactory.Workspace.deleteWorkspace( adminSession, wsName,
                        true );
                ScmFactory.Role.deleteRole( adminSession, roleName );
                ScmFactory.User.deleteUser( adminSession, username );
            }
        } finally {
            s3Client.shutdown();
            adminSession.close();
            newUserSession.close();
        }
    }
}