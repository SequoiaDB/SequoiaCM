package com.sequoiacm.s3.object;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-4265:创建SCM文件，重复关联文件
 * @Author YiPan
 * @CreateDate 2022/7/22
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4985 extends TestScmBase {
    private final String bucketName = "bucket4985";
    private final String objectKeyBase = "object4985_";
    private ScmSession session;
    private ScmWorkspace ws;
    private static List< ScmId > allFileIds = new ArrayList<>();
    private static List< String > versionFileIds = new ArrayList<>();
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private int fileNum = 20;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, fileSize );
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        // 清理环境
        S3Utils.clearBucket( session, bucketName );
        cleanFile();
        ScmFactory.Bucket.createBucket( ws, bucketName );
    }

    @Test
    public void test() throws Exception {
        createFile();
        // 单个文件关联
        try {
            ScmFactory.Bucket.attachFile( session, bucketName,
                    allFileIds.get( 0 ) );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }

        // 批量fileId关联
        List< ScmBucketAttachFailure > scmBucketAttachFailures = ScmFactory.Bucket
                .attachFile( session, bucketName, allFileIds,
                        ScmBucketAttachKeyType.FILE_ID );
        checkAttachFailure( scmBucketAttachFailures );

        // 批量fileName关联
        scmBucketAttachFailures = ScmFactory.Bucket.attachFile( session,
                bucketName, allFileIds, ScmBucketAttachKeyType.FILE_NAME );
        checkAttachFailure( scmBucketAttachFailures );
        runSuccess = true;
    }

    private void createFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( objectKeyBase + i );
            file.setContent( filePath );
            ScmId fileId = file.save();
            allFileIds.add( fileId );
            if ( i < 10 ) {
                file.updateContent( updatePath );
                versionFileIds.add( fileId.toString() );
            }
        }
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                TestTools.LocalFile.removeFile( localPath );
                S3Utils.clearBucket( session, bucketName );
                cleanFile();
            }
        } finally {
            session.close();
        }
    }

    private void checkAttachFailure(
            List< ScmBucketAttachFailure > scmBucketAttachFailures ) {
        List< String > failFiles = new ArrayList<>();
        for ( int i = 0; i < scmBucketAttachFailures.size(); i++ ) {
            ScmBucketAttachFailure scmBucketAttachFailure = scmBucketAttachFailures
                    .get( i );
            // 校验错误信息
            Assert.assertEquals( scmBucketAttachFailure.getError(),
                    ScmError.OPERATION_UNSUPPORTED );
            failFiles.add( scmBucketAttachFailure.getFileId() );
        }
        Assert.assertEqualsNoOrder( failFiles.toArray(),
                versionFileIds.toArray() );
    }

    private void cleanFile() throws ScmException {
        for ( int i = 0; i < allFileIds.size(); i++ ) {
            try {
                ScmFactory.File.deleteInstance( ws, allFileIds.get( 0 ), true );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                    throw e;
                }
            }
        }
    }

}