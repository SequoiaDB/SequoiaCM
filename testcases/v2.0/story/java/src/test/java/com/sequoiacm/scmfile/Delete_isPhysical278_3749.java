package com.sequoiacm.scmfile;

import java.io.File;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @description SCM-278:文件物理删除 SCM-3749:文件物理删除后调用isDeleted()方法
 * @author huangxiaoni
 * @createDate 2017.5.23
 * @updateUser zhangyanan
 * @updateDate 2021.9.17
 * @updateRemark
 * @version v1.0
 */

public class Delete_isPhysical278_3749 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private String fileName = "delete278_3749";
    private ScmId fileId = null;
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        fileId = ScmFileUtils.create( ws, fileName, filePath );
    }

    @Test
    private void test() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        // 校验文件isDelete状态
        Assert.assertFalse( file.isDeleted() );
        file.delete( true );
        // 校验文件isDelete状态
        Assert.assertTrue( file.isDeleted() );
        // check result
        checkResults();

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                session.close();
            }
        }
    }

    private void checkResults() throws Exception {
        BSONObject cond = new BasicBSONObject( "id", fileId.get() );
        long cnt = ScmFactory.File.countInstance( ws, ScopeType.SCOPE_CURRENT,
                cond );
        Assert.assertEquals( cnt, 0 );
        try {
            ScmFileUtils.checkData( ws, fileId, localPath, filePath );
            Assert.assertFalse( true,
                    "File is unExisted, except throw e, but success." );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                    e.getMessage() );
        }
    }
}