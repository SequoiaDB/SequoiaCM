package com.sequoiacm.s3.version;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Description SCM-4786 :: 桶开启版本控制，获取文件执行物理删除
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4786 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4786";
    private String fileName = "scmfile4786";
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
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filedata );
        ScmFileUtils.createFile( scmBucket, fileName, filedata );
        ScmFileUtils.createFile( scmBucket, fileName, filedata );
        ScmFileUtils.createFile( scmBucket, fileName, filedata );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ScmFile file = scmBucket.getFile( fileName );
        file.delete( true );
        checkFileVersion();
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

    private void checkFileVersion() throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            Assert.fail( "the file should be deleted! fileversion ="
                    + file.getMajorVersion() + " ---is deleteMarker="
                    + file.isDeleteMarker() );
        }
        cursor.close();
    }
}
