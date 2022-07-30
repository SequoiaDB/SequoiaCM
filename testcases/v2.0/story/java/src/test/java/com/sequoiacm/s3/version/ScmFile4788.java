package com.sequoiacm.s3.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-4788 :: 桶外删除文件，不指定版本号
 * @author wuyan
 * @Date 2022.07.13
 * @version 1.00
 */
public class ScmFile4788 extends TestScmBase {
    private boolean runSuccess = false;
    private String fileName = "scmfile4788";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 200;
    private byte[] updatedata = new byte[ updateSize ];
    private ScmWorkspace ws = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
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
    }

    @Test
    public void test() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.deleteVersion();

        // 检查当前文件版本信息
        int currentVersion = 1;
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        S3Utils.checkFileContent( file1, filedata );
        Assert.assertEquals( file1.getMajorVersion(), currentVersion );

        // 获取原v2版本文件不存在
        int version = 2;
        try {
            ScmFactory.File.getInstance( ws, fileId, version, 0 );
            Assert.fail( "get file with delete should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "FILE_NOT_FOUND",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}
