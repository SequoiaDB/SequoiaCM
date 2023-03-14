package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4874 :: 开启版本控制，并发指定相同版本删除相同文件
 * @author wuyan
 * @Date 2022.07.21
 * @version 1.00
 */
public class Object4874 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4874";
    private String keyName = "key4874";
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;

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
        ScmFileUtils.createFile( scmBucket, keyName, filePath );
        ScmFileUtils.createFile( scmBucket, keyName, updatePath );
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void testCreateObject() throws Exception {
        String s3DeleteVersion = "2.0";
        int scmDeleteVersion = 2;
        ThreadExecutor es = new ThreadExecutor();
        S3DeleteObject s3DeleteObject = new S3DeleteObject( keyName,
                s3DeleteVersion );
        SCMDeleteObject scmDeleteObject = new SCMDeleteObject( keyName,
                scmDeleteVersion );
        es.addWorker( s3DeleteObject );
        es.addWorker( scmDeleteObject );
        es.run();

        checkDeleteResult( s3DeleteVersion );
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

    private class S3DeleteObject {
        private String keyName;
        private String versionId;

        private S3DeleteObject( String keyName, String versionId ) {
            this.keyName = keyName;
            this.versionId = versionId;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            s3Client.deleteVersion( bucketName, keyName, versionId );
        }
    }

    private class SCMDeleteObject {
        private String keyName;
        private int versionId;

        private SCMDeleteObject( String keyName, int versionId ) {
            this.keyName = keyName;
            this.versionId = versionId;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            scmBucket.deleteFileVersion( keyName, versionId, 0 );
        }
    }

    private void checkDeleteResult( String deleteVersion ) throws Exception {
        // 获取删除版本已不存在
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                deleteVersion );
        try {
            s3Client.getObject( request );
            Assert.fail( "get object  should be fail!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
        }

        // 获取当前最新版本对象为原历史版本
        String historyVersion = "1.0";
        S3Object object = s3Client.getObject( bucketName, keyName );
        Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                historyVersion );
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }
}
