package com.sequoiacm.s3.version;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-4898 :: getNullVersionFile接口校验(非法参数校验)
 * @author wuyan
 * @Date 2022.07.23
 * @version 1.00
 */
public class ScmFile4898 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4898";
    private String fileName = "scmfile4898";
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

        // 不存在null版本文件
        try {
            scmBucket.getNullVersionFile( fileName );
            Assert.fail( "get nullVersionFile should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // fileName为null
        try {
            scmBucket.getNullVersionFile( "test4896" );
            Assert.fail( "get nullVersionFile should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // fileName为null
        try {
            scmBucket.getNullVersionFile( null );
            Assert.fail( "get nullVersionFile should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.INVALID_ARGUMENT.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // fileName为空串
        try {
            scmBucket.getNullVersionFile( "" );
            Assert.fail( "get nullVersionFile should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.INVALID_ARGUMENT.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

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
