package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
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
 * @Description SCM-4763 :: 禁用版本控制，指定版本号为null-marker获取带null-marker标记的文件
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4763 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4763";
    private String fileName = "scmfile4763";
    private String author = "author4763";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 200;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 3;
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
        new Random().nextBytes( filedata );
        new Random().nextBytes( updatedata );
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );

        // 桶禁用版本控制后再次创建同名文件
        scmBucket.suspendVersionControl();
        S3Utils.createFile( scmBucket, fileName, updatedata, author );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ScmFile file = scmBucket.getNullVersionFile( fileName );
        int currentVersion = -2;
        checkFileAttributes( file, currentVersion, updateSize );
        S3Utils.checkFileContent( file, updatedata );

        // 第一次上传文件已被删除
        int firstVersion = 1;
        try {
            ScmFactory.File.getInstance( ws, fileId, firstVersion, 0 );
            Assert.fail( "get file  should be failed!" );
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

    private void checkFileAttributes( ScmFile file, int fileVersion,
            long fileSize ) {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getSize(), fileSize );
        Assert.assertEquals( file.getAuthor(), author );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), fileVersion );
        Assert.assertTrue( file.isNullVersion() );
        Assert.assertEquals( file.getVersionSerial().getMajorSerial(), 2 );
    }
}
