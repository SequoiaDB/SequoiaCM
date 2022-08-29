package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
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
 * @Description SCM-4769 ::开启版本控制，带版本号删除历史版本文件
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4769 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4769";
    private String fileName = "scmfile4769";
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 28;
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
        int historyVersion = 1;
        scmBucket.deleteFileVersion( fileName, historyVersion, 0 );
        try {
            scmBucket.getFile( fileName, historyVersion, 0 );
            Assert.fail( "get file with historyVersion should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        int currentVersion = 2;
        ScmFile file = scmBucket.getFile( fileName, currentVersion, 0 );
        Assert.assertEquals( file.getSize(), updateSize );
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
