package com.sequoiacm.s3.version;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-4781 ::更新桶状态为禁用（enable->suspended），指定版本删除文件带deleteMarker标记
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4781 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4781";
    private String fileName = "scmfile4781";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 200;
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
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filedata );
        scmBucket.deleteFile( fileName, false );
        scmBucket.deleteFile( fileName, false );
        scmBucket.suspendVersionControl();
    }

    @Test
    public void test() throws Exception {
        int historyVersion = 2;
        scmBucket.deleteFileVersion( fileName, historyVersion, 0 );
        checkFileVersionA( historyVersion );

        int currentVersion = 3;
        scmBucket.deleteFileVersion( fileName, currentVersion, 0 );
        checkFileVersionB( currentVersion );
        // 获取当前版本为原历史版本第一次插入文件
        ScmFile file = scmBucket.getFile( fileName );
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

    private void checkFileVersionA( int historyVersion ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_HISTORY, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            if ( version == historyVersion ) {
                Assert.fail( "---the historyVersion should be deleted!" );
            } else {
                Assert.assertFalse( file.isDeleteMarker(),
                        " the file version = " + version );
                int expFileVersion = 1;
                Assert.assertEquals( version, expFileVersion );
            }
            size++;
        }
        cursor.close();

        int expFileVersionNum = 1;
        Assert.assertEquals( size, expFileVersionNum );
    }

    private void checkFileVersionB( int currentVersion ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            if ( version == currentVersion ) {
                Assert.fail( "---the currentVersion should be deleted!" );
            } else {
                Assert.assertFalse( file.isDeleteMarker() );
                int expFileVersion = 1;
                Assert.assertEquals( version, expFileVersion );
            }
            size++;
        }
        cursor.close();

        int expFileVersionNum = 1;
        Assert.assertEquals( size, expFileVersionNum );
    }

}
