package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * @Description SCM-4764 :: 指定版本号不存在，获取版本文件;SCM-4765 ::指定版本号获取不存在的版本文件
 * @author wuyan
 * @Date 2022.07.05
 * @version 1.00
 */
public class ScmFile4764_4765 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4764";
    private String fileName = "scmfile4764";
    private SiteWrapper site = null;
    private ScmWorkspace ws = null;
    private int fileSize = 1024;
    private byte[] filedata = new byte[ fileSize ];
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        new Random().nextBytes( filedata );
        S3Utils.createFile( scmBucket, fileName, filedata );
    }

    @Test
    public void test() throws Exception {
        // test4764：指定版本号不存在
        try {
            int errorVersion = 2;
            scmBucket.getFile( fileName, errorVersion, 0 );
            Assert.fail( "get file with error version should be failed!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // test4765：指定版本号获取不存在的版本文件
        try {
            int currentVersion = 1;
            scmBucket.getFile( "errorFile4765", currentVersion, 0 );
            Assert.fail( "get file with error name should be failed!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
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
