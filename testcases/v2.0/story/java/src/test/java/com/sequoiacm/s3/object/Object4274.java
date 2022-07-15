package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sequoiacm.client.core.*;
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
 * @descreption SCM-4274 :: S3接口创建对象，设置自由标签
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4274 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4274";
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4274";
    private String key = "aa/bb/object4274";
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
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );

        int siteNum = ScmInfo.getSiteNum();
        ws = ScmWorkspaceUtil.createS3WS( session, wsName );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    public void test() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );

        //
        Map< String, String > xMeta = new HashMap<>();
        xMeta.put( user_meta_key_a, user_meta_value_a );
        xMeta.put( user_meta_key_b, user_meta_value_b );
        xMeta.put( user_meta_key_c, user_meta_value_c );
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setUserMetadata( xMeta );
        PutObjectRequest request = new PutObjectRequest( bucketName, key,
                new File( filePath ) );
        request.setMetadata( metaData );
        s3Client.putObject( request );

        ScmFile file = bucket.getFile( key );
        Map< String, String > userMeta = file.getCustomMetadata();
        Assert.assertEquals( userMeta.get( user_meta_key_a ),
                user_meta_value_a );
        Assert.assertEquals( userMeta.get( user_meta_key_b ),
                user_meta_value_b );
        Assert.assertEquals( userMeta.get( user_meta_key_c ), "" );

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
