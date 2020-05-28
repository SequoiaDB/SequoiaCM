package com.sequoiacm.scmfile;

import java.io.File;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-138:结果集为空，调用hasNext和getNext SCM-139:close未关闭的游标，并重复close
 *            SCM-217:不调用hasNext，直接调用getNext获取文件 SCM-220:重复关闭cursor后再关闭session
 * @author huangxiaoni init
 * @date 2017.4.6
 */

public class Cursor138A139A217A220 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private ScmWorkspace ws = null;

    private int fileSize = 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "scmfile138";
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            fileId = ScmFileUtils.create( ws, fileName, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCursorByGetNext() {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            ScopeType scopeType = ScopeType.SCOPE_CURRENT;
            BSONObject condition = new BasicBSONObject(
                    ScmAttributeName.File.FILE_ID, fileId.get() );
            cursor = ScmFactory.File.listInstance( ws, scopeType, condition );

            ScmFileBasicInfo info = cursor.getNext();
            Assert.assertEquals( info.getFileId().get(), fileId.get() );

            cursor.close();
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCursorByBlankResult() {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            ScopeType scopeType = ScopeType.SCOPE_CURRENT;
            BSONObject condition = new BasicBSONObject( "", "" );
            cursor = ScmFactory.File.listInstance( ws, scopeType, condition );
            boolean rc = cursor.hasNext();
            Assert.assertEquals( rc, false );

            ScmFileBasicInfo rcInfo = cursor.getNext();
            Assert.assertEquals( rcInfo, null );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            cursor.close();
        }
        runSuccess2 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCursorByRepeatClose() {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            ScopeType scopeType = ScopeType.SCOPE_CURRENT;
            BSONObject condition = new BasicBSONObject(
                    ScmAttributeName.File.FILE_ID, fileId.get() );
            cursor = ScmFactory.File.listInstance( ws, scopeType, condition );

            int size = 0;
            while ( cursor.hasNext() ) {
                cursor.getNext();
                size++;
            }
            Assert.assertEquals( size, 1 );
            cursor.close();
            cursor.close();
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess3 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess1 || runSuccess2 || runSuccess3
                    || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

}