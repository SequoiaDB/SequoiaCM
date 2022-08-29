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

import java.util.Random;

/**
 * @Description SCM-4772 ::开启版本控制，指定版本号删除文件，该文件为删除标记(指定版本号为历史版本)
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4772B extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4772b";
    private String fileName = "scmfile4772b";
    private ScmId fileId = null;
    private byte[] filedata = new byte[ 1024 ];
    private SiteWrapper site = null;
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
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );
        scmBucket.deleteFile( fileName, false );
        scmBucket.deleteFile( fileName, false );
        scmBucket.deleteFile( fileName, false );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        int historyVersion = 2;
        scmBucket.deleteFileVersion( fileName, historyVersion, 0 );
        checkFileVersion( historyVersion );

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

    private void checkFileVersion( int deleteVersion ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int fileVersion = file.getMajorVersion();
            if ( fileVersion == 1 ) {
                Assert.assertFalse( file.isDeleteMarker() );
            } else {
                Assert.assertTrue( file.isDeleteMarker(),
                        "---fileVersion=" + fileVersion );
            }
            if ( fileVersion == deleteVersion ) {
                Assert.fail( "---the version should be deleted!" );
            }
            Assert.assertEquals( file.getFileName(), fileName );
            size++;
        }
        cursor.close();

        int expFileVersionNum = 3;
        Assert.assertEquals( size, expFileVersionNum );
    }
}
