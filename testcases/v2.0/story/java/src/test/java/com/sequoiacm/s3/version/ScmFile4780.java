package com.sequoiacm.s3.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
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
 * @Description SCM-4780 :: 更新桶状态为禁用（enable->suspended），不指定版本删除文件为null-marker版本
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4780 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4780";
    private String fileName = "scmfile4780";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 200;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1;
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
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );
        scmBucket.suspendVersionControl();
        S3Utils.createFile( scmBucket, fileName, updatedata );
    }

    @Test
    public void test() throws Exception {
        ScmFile file = scmBucket.getNullVersionFile( fileName );
        file.delete( false );

        try {
            scmBucket.getFile( fileName );
            Assert.fail( "get file with historyVersion should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 获取文件列表中只有当前版本文件
        int historyVersion = 1;
        int currentVersion = -2;
        checkFileVersion( currentVersion, historyVersion );
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

    private void checkFileVersion( int currentVersion, int historyVersion )
            throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            System.out.println( "---version = " + version );
            if ( version == currentVersion ) {
                Assert.assertTrue( file.isDeleteMarker() );
                String serivalVersion = "3.0";
                Assert.assertEquals( serivalVersion,
                        file.getVersionSerial().getMajorSerial() + "."
                                + file.getVersionSerial().getMinorSerial() );
            } else {
                Assert.assertFalse( file.isDeleteMarker(),
                        "---the file version =" + version );
                Assert.assertEquals( version, historyVersion );
                ScmFile hisFile = scmBucket.getFile( fileName, historyVersion,
                        0 );
                Assert.assertEquals( hisFile.getSize(), fileSize );
                S3Utils.checkFileContent( hisFile, filedata );
            }
            size++;
        }
        cursor.close();

        int expFileVersionNum = 2;
        Assert.assertEquals( size, expFileVersionNum );
    }
}
