package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4871 ::开启版本控制，并发获取相同文件
 * @author wuyan
 * @Date 2022.07.20
 * @version 1.00
 */
public class Object4871 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4871";
    private String keyName = "key4871";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 2;
    private int updateSize = 1024 * 1024 * 3;
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
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, keyName, filePath );
        S3Utils.createFile( scmBucket, keyName, updatePath );
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        S3GetObject s3GetObject = new S3GetObject( keyName );
        SCMGetFile scmGetFile = new SCMGetFile( keyName );
        es.addWorker( s3GetObject );
        es.addWorker( scmGetFile );
        es.run();

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

    private class S3GetObject {
        private String keyName;
        private S3Object s3Object;

        private S3GetObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() {
            s3Object = s3Client.getObject( bucketName, keyName );

        }

        @ExecuteOrder(step = 2)
        private void checkResult() throws Exception {
            // 检查对象元数据属性
            ObjectMetadata metadata = s3Object.getObjectMetadata();
            String currentVersion = "2.0";
            Assert.assertEquals( metadata.getVersionId(), currentVersion );
            Assert.assertEquals( s3Object.getBucketName(), bucketName );
            Assert.assertEquals( metadata.getETag(),
                    TestTools.getMD5( updatePath ) );
            // 检查对象内容
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, keyName );
            Assert.assertEquals( downfileMd5, TestTools.getMD5( updatePath ) );
        }
    }

    private class SCMGetFile {
        String keyName;
        ScmFile scmFile;

        private SCMGetFile( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            scmFile = scmBucket.getFile( keyName );
        }

        @ExecuteOrder(step = 2)
        private void checkResult() throws Exception {
            // 检查对象元数据属性
            Assert.assertEquals( scmFile.getWorkspaceName(), s3WorkSpaces );
            Assert.assertEquals( scmFile.getFileId(), fileId );
            Assert.assertEquals( scmFile.getFileName(), keyName );
            Assert.assertEquals( scmFile.getTitle(), keyName );
            Assert.assertEquals( scmFile.getSize(), updateSize );
            Assert.assertEquals( scmFile.getMajorVersion(), 2 );
            // 检查对象内容
            S3Utils.checkFileContent( scmFile, updatePath, localPath );
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, keyName );
            Assert.assertEquals( downfileMd5, TestTools.getMD5( updatePath ) );
        }
    }
}
