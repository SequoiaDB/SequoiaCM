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
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * @Description SCM-4782 :: 设置不同版本控制状态，删除文件
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4782 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4782";
    private String fileName = "scmfile4782";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 200;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1;
    private byte[] updatedata = new byte[ updateSize ];
    private int updateSize1 = 1024 * 3;
    private byte[] updatedata1 = new byte[ updateSize1 ];
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
        new Random().nextBytes( updatedata1 );
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );
        scmBucket.enableVersionControl();
        S3Utils.createFile( scmBucket, fileName, updatedata );
        scmBucket.suspendVersionControl();
        S3Utils.createFile( scmBucket, fileName, updatedata1 );
        scmBucket.enableVersionControl();
        S3Utils.createFile( scmBucket, fileName, filedata );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        scmBucket.deleteFile( fileName, true );
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
                ScmType.ScopeType.SCOPE_HISTORY, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            Assert.fail(
                    "---the historyVersion should be deleted! the file version = "
                            + version );
            size++;
        }
        cursor.close();

        int expFileVersionNum = 0;
        Assert.assertEquals( size, expFileVersionNum );
    }

}
