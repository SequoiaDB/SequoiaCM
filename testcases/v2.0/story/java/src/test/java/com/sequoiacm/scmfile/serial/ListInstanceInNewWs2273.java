package com.sequoiacm.scmfile.serial;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-2273:在新增ws下带查询条件查询文件列表
 * @Author fanyu
 * @Date 2018-10-15
 * @Version 1.00
 */

public class ListInstanceInNewWs2273 extends TestScmBase {
    private boolean runSuccess = false;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;
    private String name = "ListInstanceInNewWs2273";

    private SiteWrapper site = null;
    private ScmSession session = null;

    private String wsName = "test";
    private int num = 200;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        } catch ( IOException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        this.prepareWs();
        WriteFile wThread = new WriteFile();
        wThread.start( num );
        boolean wflag = wThread.isSuccess();
        Assert.assertEquals( wflag, true, wThread.getErrorMsg() );
        this.listByCond();
        ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void listByCond() {
        int count = 0;
        try {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsName, session );
            ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                    .listInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            while ( cursor.hasNext() ) {
                cursor.getNext();
                count++;
            }
            Assert.assertEquals( count, num );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void prepareWs() {
        try {
            ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
            ScmWorkspaceUtil.wsSetPriority( session, wsName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    class WriteFile extends TestThreadBase {
        @Override
        public void exec() {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsName, session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( name + "_" + UUID.randomUUID() );
                file.setAuthor( name );
                file.save();
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
