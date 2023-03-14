package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * @Description SCM-4767 :: 桶开启版本控制，不带版本号删除文件 （设置isPhyscial为false，非物理删除）
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4767A extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4767";
    private String fileName = "scmfile4767";
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 10;
    private byte[] updatedata = new byte[ updateSize ];
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
        new Random().nextBytes( filedata );
        new Random().nextBytes( updatedata );
        ScmFileUtils.createFile( scmBucket, fileName, filedata );
        ScmFileUtils.createFile( scmBucket, fileName, updatedata );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        scmBucket.deleteFile( fileName, false );
        // 获取当前版本文件不存在为删除标记
        try {
            scmBucket.getFile( fileName );
            Assert.fail( "get file with deleteMarker should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 检查删除文件移到历史版本，版本号为2
        int historyVersion1 = 2;
        ScmFile file1 = scmBucket.getFile( fileName, historyVersion1, 0 );
        S3Utils.checkFileContent( file1, updatedata );

        // 检查原历史版本文件，版本号为1
        int historyVersion2 = 1;
        ScmFile file2 = scmBucket.getFile( fileName, historyVersion2, 0 );
        S3Utils.checkFileContent( file2, filedata );
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
