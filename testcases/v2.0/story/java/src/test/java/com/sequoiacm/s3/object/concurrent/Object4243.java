package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @descreption SCM-4243 :: 并发关联相同文件到桶
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4243 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4243";
    private String bucketNameA = "bucket4243-a";
    private String bucketNameB = "bucket4243-b";
    private String key = "aa/bb/object4243";
    private ScmId fileId;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        cleanBuckets( bucketName );
        ScmBucket bucketA = ScmFactory.Bucket.createBucket( ws, bucketNameA );
        bucketA.enableVersionControl();
        ScmBucket bucketB = ScmFactory.Bucket.createBucket( ws, bucketNameB );
        bucketB.enableVersionControl();

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // scm create file
        ScmFile file = createScmFile();
        fileId = file.getFileId();

        ThreadExecutor te = new ThreadExecutor();
        AttachFile t1 = new AttachFile( fileId, bucketNameA );
        AttachFile t2 = new AttachFile( fileId, bucketNameA );
        AttachFile t3 = new AttachFile( fileId, bucketNameB );
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.addWorker( t3 );
        te.run();

        if ( t3.getRetCode() != 0 ) {
            Assert.assertTrue( s3Client.doesObjectExist( bucketNameA, key ) );
            Assert.assertFalse( s3Client.doesObjectExist( bucketNameB, key ) );
        } else {
            Assert.assertTrue( s3Client.doesObjectExist( bucketNameB, key ) );
            Assert.assertFalse( s3Client.doesObjectExist( bucketNameA, key ) );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket(s3Client, bucketName);
                S3Utils.clearBucket(s3Client, bucketNameA);
                S3Utils.clearBucket(s3Client, bucketNameB);
                TestTools.LocalFile.removeFile( localPath );
                try {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                        throw e;
                    }
                }
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

    private ScmFile createScmFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( key );
        file.setAuthor( "author4243" );
        file.save( new ScmUploadConf( true, true ) );

        return file;
    }

    private void cleanBuckets( String bucketPrefix ) {
        List< Bucket > buckets = s3Client.listBuckets();
        // bucket is not empty
        for ( int i = 0; i < buckets.size(); i++ ) {
            if ( buckets.get( i ).getName().startsWith( bucketPrefix ) ) {
                S3Utils.clearBucket( s3Client, buckets.get( i ).getName() );
            }
        }
    }

    class AttachFile extends ResultStore {
        private String attachBucketName;
        private ScmId attachFileId;

        AttachFile( ScmId fileId, String bucketName ) {
            this.attachBucketName = bucketName;
            this.attachFileId = fileId;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmFactory.Bucket.attachFile( session, attachBucketName,
                        attachFileId );
            } catch ( ScmException e ) {
                saveResult( e.getError().getErrorCode(), e );
            }
        }
    }
}
