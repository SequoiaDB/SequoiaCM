package com.sequoiacm.directory;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1201 :: ScmFactory.Directory中的getInstance参数校验 
 * @author fanyu
 * @Date:2018年4月26日
 * @version:1.0
 */
public class GetInstance_Para1201 extends TestScmBase {
    private boolean runSuccess1;
    private boolean runSuccess2;
    private boolean runSuccess3;
    private boolean runSuccess4;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/GetInstance_Para1201";
    private ScmDirectory dir;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            dir = ScmFactory.Directory.createInstance( ws, dirBasePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testGetChineseDir() {
        String pathName = "文件夹a_1201";
        try {
            dir.createSubdirectory( pathName );
            ScmDirectory subdir1 = ScmFactory.Directory
                    .getInstance( ws, dirBasePath + "/" + pathName );
            //Assert.assertEquals(subdir1.getName(), pathName);
            Assert.assertEquals( subdir1.getPath(),
                    dirBasePath + "/" + pathName + "/" );
            subdir1.delete();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testGetRelativeDir() {
        String path = "1201";
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, path );
            Assert.fail( "expect fail but act success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess2 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateWsIsNull() {
        try {
            ScmDirectory dir = ScmFactory.Directory
                    .getInstance( null, dirBasePath );
            Assert.fail( "expect fail but act success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess2 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testPaPathInexist() {
        String paPath = dirBasePath + "/testPaPathInexist_1201";
        try {
            ScmDirectory dir = ScmFactory.Directory
                    .createInstance( ws, paPath );
            dir.delete();
            ScmDirectory subdir = ScmFactory.Directory
                    .getInstance( ws, paPath );
            Assert.fail( "expect fail but success," + subdir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess3 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testPathIsNull() {
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, null );
            Assert.fail( "expect fail but act success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess4 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess1 && runSuccess2 && runSuccess3 && runSuccess4 ||
                    TestScmBase.forceClear ) {
                ScmFactory.Directory.deleteInstance( ws, dirBasePath );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
