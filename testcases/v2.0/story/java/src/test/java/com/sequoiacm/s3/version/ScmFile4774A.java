package com.sequoiacm.s3.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * @Description SCM-4774 :: 禁用版本控制，不带版本号删除文件 （设置isPhyscial为false，非物理删除）
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4774A extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4774";
    private String fileName = "scmfile4774";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 100;
    private byte[] filedata = new byte[ fileSize ];
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
        scmBucket.suspendVersionControl();
        new Random().nextBytes( filedata );
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );
    }

    @Test
    public void test() throws Exception {
        scmBucket.deleteFile( fileName, false );
        // 获取当前版本文件不存在为删除标记
        try {
            scmBucket.getFile( fileName );
            Assert.fail( "get file with deleteMarker should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "FILE_NOT_FOUND",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 检查元数据列表新增一条删除标记版本
        checkFileVersion();
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

    private void checkFileVersion() throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            Assert.assertTrue( file.isDeleteMarker(),
                    "---the version =" + file.getMajorVersion() );
            Assert.assertEquals( file.getMajorVersion(), -2 );
            Assert.assertEquals( file.getVersionSerial().getMajorSerial(), 2 );
            Assert.assertTrue( file.isNullVersion() );
            size++;
        }
        cursor.close();

        int expFileVersionNum = 1;
        Assert.assertEquals( size, expFileVersionNum );
    }
}
