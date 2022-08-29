package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
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
import java.util.List;

/**
 * @Description: SCM-4875 :: 开启版本控制，并发指定不同版本删除相同文件
 * @author wuyan
 * @Date 2022.07.21
 * @version 1.00
 */
public class Object4875 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4875";
    private String keyName = "key_4875";
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
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        int versionNum = 10;
        for ( int i = 0; i < versionNum; i++ ) {
            if ( i % 2 == 0 ) {
                S3Utils.createFile( scmBucket, keyName, filePath );
            } else {
                s3Client.putObject( bucketName, keyName,
                        new File( updatePath ) );
            }
        }

        ThreadExecutor es = new ThreadExecutor();
        for ( int i = 1; i < versionNum + 1; i++ ) {
            if ( i % 2 == 0 ) {
                String s3DeleteVersion = i + ".0";
                S3DeleteObject s3DeleteObject = new S3DeleteObject( keyName,
                        s3DeleteVersion );
                es.addWorker( s3DeleteObject );
            } else {
                SCMDeleteObject scmDeleteObject = new SCMDeleteObject( keyName,
                        i );
                es.addWorker( scmDeleteObject );
            }
        }
        es.run();

        checkDeleteResult();
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
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteVersion( bucketName, keyName, versionId );
            } finally {
                s3Client.shutdown();
            }

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
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                bucket.deleteFileVersion( keyName, versionId, 0 );
            } finally {
                session.close();
            }

        }
    }

    private void checkDeleteResult() throws Exception {
        // 获取删除版本已不存在
        VersionListing verList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        List< S3VersionSummary > objectVersionList = verList
                .getVersionSummaries();
        Assert.assertEquals( objectVersionList.size(), 0 );
        // scm端获取桶中文件数为0
        Assert.assertEquals( scmBucket.countFile( null ), 0 );
    }
}
