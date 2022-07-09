package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-4237:S3接口和SCM API并发删除S3文件
 * @Author YiPan
 * @CreateDate 2022/5/16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4237 extends TestScmBase {
    private final String bucketName = "bucket4237";
    private final String objectKey = "object4237";
    private AmazonS3 s3Client;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;
    private ScmSession session;
    private ScmWorkspace ws;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws Exception {
        // 创建桶，创建s3文件
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectKey, new File( filePath ) );

        // 并发删除文件
        ScmId fileId = S3Utils.queryS3Object( ws, objectKey );
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ScmDeleteObject( fileId ) );
        te.addWorker( new S3DeleteObject() );
        te.run();

        // 校验删除结果
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, objectKey ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            session.close();
            s3Client.shutdown();
        }
    }

    private class ScmDeleteObject extends ResultStore {
        private ScmId fileId;

        public ScmDeleteObject( ScmId fileId ) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            } catch ( ScmException e ) {
                if ( !( e.getError().equals( ScmError.FILE_NOT_FOUND ) ) ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }

    private class S3DeleteObject extends ResultStore {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteObject( bucketName, objectKey );
            } finally {
                s3Client.shutdown();
            }
        }
    }
}