package com.sequoiacm.s3.bucket;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @descreption SCM-4252 :: 使用SCM API删除桶，桶内存在文件
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4252 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4252";
    private ScmWorkspace ws_test = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4252";
    private String key = "aa/bb/object4252";
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

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        int siteNum = ScmInfo.getSiteNum();

        ws_test = ScmWorkspaceUtil.createS3WS( session, wsName );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws ScmException, IOException {
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws_test,
                bucketName );
        ScmFile file = bucket.createFile( key );
        file.setContent( filePath );
        file.save( new ScmUploadConf( true, true ) );

        try {
            ScmFactory.Bucket.deleteBucket( session, bucketName );
            Assert.fail( "Delete bucket：" + bucketName + " should failed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError().getErrorType(),
                    "BUCKET_NOT_EMPTY" );
        }

        bucket.deleteFile( key, true );

        ScmFactory.Bucket.deleteBucket( session, bucketName );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                ScmWorkspaceUtil.deleteWs( wsName, session );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
