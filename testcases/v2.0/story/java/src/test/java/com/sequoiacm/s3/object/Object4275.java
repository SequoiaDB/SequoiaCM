package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @descreption SCM-4275 :: SCM API创建s3文件，设置自由标签
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4275 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4275";
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4275";
    private String key = "aa/bb/object4275";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private String user_meta_key_a = "test_a";
    private String user_meta_key_b = "test_b";
    private String user_meta_key_c = "test_c";
    private String user_meta_value_a = "aaaa";
    private String user_meta_value_b = "";
    private String user_meta_value_c = null;

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

        ws = ScmWorkspaceUtil.createS3WS( session, wsName );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    public void test() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        s3Client.doesBucketExistV2( bucketName );
        ScmFile file = bucket.createFile( key );
        file.setContent( filePath );
        Map< String, String > xMeta = new HashMap<>();
        xMeta.put( user_meta_key_a, user_meta_value_a );
        xMeta.put( user_meta_key_b, user_meta_value_b );
        xMeta.put( user_meta_key_c, user_meta_value_c );
        file.setCustomMetadata( xMeta );
        file.save( new ScmUploadConf( true, true ) );

        S3Object object = s3Client.getObject( bucketName, key );

        Map< String, String > userMeta = object.getObjectMetadata()
                .getUserMetadata();
        Assert.assertEquals( userMeta.get( user_meta_key_a ),
                user_meta_value_a );
        Assert.assertEquals( userMeta.get( user_meta_key_b ),
                user_meta_value_b );
        Assert.assertEquals( userMeta.get( user_meta_key_c ),
                "" );

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
