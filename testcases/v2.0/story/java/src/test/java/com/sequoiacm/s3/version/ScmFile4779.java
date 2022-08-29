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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-4779:更新桶状态为开启（disable->enabled），指定历史版本删除文件（该文件为null-marker版本）
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4779 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4779";
    private String fileName = "scmfile4779";
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
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );
        scmBucket.enableVersionControl();
        S3Utils.createFile( scmBucket, fileName, updatedata );
    }

    @Test
    public void test() throws Exception {
        int historyVersion = -2;
        ScmFile file = scmBucket.getNullVersionFile(fileName);
        file.deleteVersion( historyVersion, 0 );

        try {
            scmBucket.getFile( fileName, historyVersion, 0 );
            Assert.fail( "get file with historyVersion should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 获取文件列表中只有当前版本文件
        int currentVersion = 2;
        checkFileVersion( currentVersion );
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

    private void checkFileVersion( int currentVersion ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            Assert.assertEquals( version, currentVersion );
            size++;
        }
        cursor.close();

        int expFileVersionNum = 1;
        Assert.assertEquals( size, expFileVersionNum );
    }
}
