package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @descreption SCM-4271 :: 通过S3接口创建文件，解除文件关联
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4271 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4271";
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4271";
    private String key = "aa/bb/object4271";
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

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        s3Client.createBucket( new CreateBucketRequest( bucketName, wsName ) );
        s3Client.putObject( bucketName, key, new File( filePath ) );

        ScmFile s3File = ScmFactory.Bucket.getBucket( session, bucketName )
                .getFile( key );
        ScmFactory.Bucket.detachFile( session, bucketName, key );

        ObjectListing objectListing = s3Client.listObjects( bucketName );
        Assert.assertEquals( objectListing.getObjectSummaries().size(), 0 );

        ScmFile detachedFile = ScmFactory.File.getInstance( ws,
                s3File.getFileId() );
        String downloadPath = localPath + File.separator + "downLoadFile_"
                + fileSize + ".txt";
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
                S3Utils.clearBucket( s3Client, bucketName );
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
