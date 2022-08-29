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
 * @Description SCM-4767 :: 桶开启版本控制，不带版本号删除文件 （设置isPhyscial为true，物理删除）
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4767B extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4767b";
    private String fileName = "scmfile4767b";
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 8;
    private byte[] updatedata = new byte[ updateSize ];
    private ScmWorkspace ws = null;
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
        new Random().nextBytes( updatedata );
        S3Utils.createFile( scmBucket, fileName, filedata );
        S3Utils.createFile( scmBucket, fileName, updatedata );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        scmBucket.deleteFile( fileName, true );

        // 获取当前版本文件不存在
        try {
            scmBucket.getFile( fileName );
            Assert.fail( "get file with deleteMarker should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 检查文件所有版本都不存
        int version1 = 1;
        int version2 = 2;
        try {
            scmBucket.getFile( fileName, version1, 0 );
            Assert.fail( "get file should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        try {
            scmBucket.getFile( fileName, version2, 0 );
            Assert.fail( "get file should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 检查桶中不存在文件
        long fileNum = 0;
        Assert.assertEquals( scmBucket.countFile( null ), fileNum );
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
