package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ObjectListing;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * @descreption SCM-4272 :: 关联SCM文件到桶中，解除文件关联
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4272 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4272";
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4272";
    private String key = "aa/bb/object4272";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );

        int siteNum = ScmInfo.getSiteNum();
        ws = ScmWorkspaceUtil.createS3WS( session, wsName );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    public void test() throws Exception {
        ScmFactory.Bucket.createBucket( ws, bucketName );
        S3Utils.updateBucketVersionConfig( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );

        ScmId fileId;
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( key );
        file.setContent( filePath );
        file.save( new ScmUploadConf( true, true ) );
        fileId = file.getFileId();

        ScmFactory.Bucket.attachFile( session, bucketName, fileId );

        ObjectListing objectListing = s3Client.listObjects( bucketName );
        Assert.assertEquals( objectListing.getObjectSummaries().size(), 1 );

        ScmFactory.Bucket.detachFile( session, bucketName, key );

        objectListing = s3Client.listObjects( bucketName );
        Assert.assertEquals( objectListing.getObjectSummaries().size(), 0 );

        ScmFile detachedFile = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileOutputStream = new FileOutputStream( downloadPath );
        detachedFile.getContent( fileOutputStream );
        fileOutputStream.close();

        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, bucketName );
                ScmWorkspaceUtil.deleteWs( wsName, session );
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
}
