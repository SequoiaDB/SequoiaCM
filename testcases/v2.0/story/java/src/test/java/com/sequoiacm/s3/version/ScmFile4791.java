package com.sequoiacm.s3.version;

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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description SCM-4791 :: 获取版本文件执行物理删除
 * @author wuyan
 * @Date 2022.07.13
 * @version 1.00
 */
public class ScmFile4791 extends TestScmBase {
    private boolean runSuccess = false;
    private String fileName = "文件scmfile4791";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 2;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 110;
    private byte[] updatedata = new byte[ updateSize ];
    private ScmWorkspace ws = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT, cond );
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo fileInfo = cursor.getNext();
            ScmId fileId = fileInfo.getFileId();
            ScmFactory.File.deleteInstance( ws, fileId, true );
        }
        cursor.close();

        fileId = VersionUtils.createFileByStream( ws, fileName, filedata );
        VersionUtils.updateContentByStream( ws, fileId, updatedata );
        VersionUtils.updateContentByStream( ws, fileId, filedata );
    }

    @Test
    public void test() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.delete( true );
        checkDeleteResult( ws );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkDeleteResult( ScmWorkspace ws ) throws ScmException {
        BSONObject fileCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        long currentCount = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT, fileCond );
        long historyCount = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_HISTORY, fileCond );
        long expCount = 0;
        Assert.assertEquals( currentCount, expCount,
                " the currentVersion file must be delete" );
        Assert.assertEquals( historyCount, expCount,
                " the historyVersion file must be delete" );
    }

}
