package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

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
 * @Description:SCM-1198 :: ScmFactory.Directory中的createInstance参数校验
 * @author fanyu
 * @Date:2018年4月27日
 * @version:1.0
 */
public class CreateInstance_Param1198 extends TestScmBase {
    private boolean runSuccess1;
    private boolean runSuccess2;
    private boolean runSuccess3;
    private boolean runSuccess4;
    private boolean runSuccess5;
    private boolean runSuccess6;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/CreateInstance_Param1198";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            deleteDir( ws, dirBasePath );
            ScmFactory.Directory.createInstance( ws, dirBasePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateChineseDir() {
        String path = dirBasePath + "/文件夹a_1198";
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, path );
            ScmDirectory checkDir1 = ScmFactory.Directory.getInstance( ws,
                    path );
            Assert.assertEquals( checkDir1.getPath(), path + "/" );
            dir.createSubdirectory( "文件夹b_1198" );
            ScmDirectory checkDir2 = ScmFactory.Directory.getInstance( ws,
                    path + "/文件夹b_1198" );
            Assert.assertEquals( checkDir2.getPath(), path + "/文件夹b_1198/" );
            ScmFactory.Directory.deleteInstance( ws, path + "/文件夹b_1198" );
            ScmFactory.Directory.deleteInstance( ws, path );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRlativeDir() {
        String path = "1198";
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, path );
            Assert.fail( "expect fail but act success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreatePathIsNull() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, null );
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
        String paPath = dirBasePath + "/testPaPathInexist";
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                    paPath );
            dir.delete();
            ScmDirectory subdir = dir.createSubdirectory( "testPaPathInexist" );
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
    private void testWsIsNull() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( null, "a" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess4 = true;
    }

    // bug:248
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void testNameIsDot() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, "." );
            ScmFactory.Directory.getInstance( ws, "." );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess5 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasSprit() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, "//" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess6 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasBackslash() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, "\\" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess6 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasStar() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, "*" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess6 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasQuestionMark() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, "12?" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess6 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasLessThanSign() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                    "qwer<" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess6 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasGreatThanSign() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                    "qwer<" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess6 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasOrSign() {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                    "|qwer" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess6 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess1 && runSuccess2 && runSuccess3 && runSuccess4
                    && runSuccess5 && runSuccess6 || TestScmBase.forceClear ) {
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

    private void deleteDir( ScmWorkspace ws, String dirPath ) {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_NOT_EMPTY ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private List< String > getSubPaths( String path ) {
        String ele = "/";
        String[] arry = path.split( "/" );
        List< String > pathList = new ArrayList< String >();
        for ( int i = 1; i < arry.length; i++ ) {
            ele = ele + arry[ i ];
            pathList.add( ele );
            ele = ele + "/";
        }
        return pathList;
    }
}
