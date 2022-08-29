package com.sequoiacm.s3.version;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-4783 :: 开启版本控制，重复多次删除相同文件
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4783 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4783";
    private String fileName = "scmfile4783";
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
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        int deleteCount = 20;
        for ( int i = 0; i < deleteCount; i++ ) {
            scmBucket.deleteFile( fileName, false );
        }

        checkFileVersion( deleteCount );
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

    private void checkFileVersion( int deleteMarkerNum ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        int noDeleteMarkerVersion = 1;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            if ( version == noDeleteMarkerVersion ) {
                Assert.assertFalse( file.isDeleteMarker(),
                        " the file version = " + version );
            } else {
                Assert.assertTrue( file.isDeleteMarker(),
                        " the file version = " + version );
            }
            size++;
        }
        cursor.close();
        Assert.assertEquals( size, deleteMarkerNum + 1 );
    }

}
