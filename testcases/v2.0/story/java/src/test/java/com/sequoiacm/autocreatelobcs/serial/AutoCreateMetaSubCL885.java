package com.sequoiacm.autocreatelobcs.serial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @FileName SCM-885:元数据子表不存在，并发写文件
 * @Author huangxiaoni
 * @Date 2017-10-10
 */

public class AutoCreateMetaSubCL885 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;
    private String fileName = "AutoCreateMetaSubCL885";
    private boolean runSuccess = false;
    private String wsName = "test_885_0";
    private ScmWorkspace ws;
    private AtomicInteger atom = new AtomicInteger( 0 );
    private List< ScmId > fileIdList = Collections
            .synchronizedList( new ArrayList< ScmId >() );

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( site );
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        } catch ( ScmException e ) {
            if ( ScmError.WORKSPACE_NOT_EXIST != e.getError() ) {
                throw e;
            }
        }
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException {
        WriteFile writeFile = new WriteFile();
        writeFile.start( 10 );
        Assert.assertTrue( writeFile.isSuccess(), writeFile.getErrorMsg() );
        // check result
        for ( ScmId fileId : fileIdList ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getAuthor(), fileName, fileId.get() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class WriteFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            Calendar cal = Calendar.getInstance();
            ScmSession ss = null;
            try {
                ss = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        ss );
                cal.set( Calendar.YEAR,
                        cal.get( Calendar.YEAR ) - atom.getAndIncrement() );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + "_" + UUID.randomUUID() );
                file.setAuthor( fileName );
                file.setCreateTime( cal.getTime() );
                fileIdList.add( file.save() );
            } finally {
                if ( null != ss ) {
                    ss.close();
                }
            }
        }
    }
}