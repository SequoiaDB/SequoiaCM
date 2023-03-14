package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Description SCM-4897 :: deleteFile接口校验(非法参数校验)
 * @author wuyan
 * @Date 2022.07.23
 * @version 1.00
 */
public class ScmFile4897 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4897";
    private String fileName = "scmfile4897";
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        ScmFileUtils.createFile( scmBucket, fileName, filedata );
    }

    @Test
    public void test() throws Exception {
        // fileName为null
        try {
            scmBucket.deleteFile( null, false );
            Assert.fail( "delete file should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "INVALID_ARGUMENT",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // fileName为空串
        try {
            scmBucket.deleteFile( "", false );
            Assert.fail( "delete file should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "INVALID_ARGUMENT",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 获取文件还存在未删除
        ScmFile file = scmBucket.getFile( fileName );
        Assert.assertEquals( file.getMajorVersion(), 1 );
        Assert.assertEquals( file.getSize(), fileSize );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
