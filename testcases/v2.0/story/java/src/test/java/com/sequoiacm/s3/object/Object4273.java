package com.sequoiacm.s3.object;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4273 :: 重复解除文件关联
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4273 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4273";
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4273";
    private String key = "aa/bb/object4273";
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
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );

        int siteNum = ScmInfo.getSiteNum();
        ws = ScmWorkspaceUtil.createS3WS( session, wsName );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    public void test() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );

        ScmFile file = bucket.createFile( key );
        file.setContent( filePath );
        file.save( new ScmUploadConf( true, true ) );

        ScmFactory.Bucket.detachFile( session, bucketName, key );

        try {
            ScmFactory.Bucket.detachFile( session, bucketName, key );
            Assert.fail("File should not found." );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError().getErrorType(),
                    "FILE_NOT_FOUND" );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket(session, bucketName);
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
