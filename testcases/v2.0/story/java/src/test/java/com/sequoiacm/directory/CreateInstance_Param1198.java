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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1198 :: ScmFactory.Directory中的createInstance参数校验
 * @author fanyu
 * @Date:2018年4月27日
 * @version:1.0
 */
public class CreateInstance_Param1198 extends TestScmBase {
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/CreateInstance_Param1198";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        deleteDir( ws, dirBasePath );
        ScmFactory.Directory.createInstance( ws, dirBasePath );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateChineseDir() throws ScmException {
        String path = "/ dir1198 中文 .!_ test@#$()+";
        ScmDirectory dir = ScmFactory.Directory.createInstance( ws, path );
        ScmDirectory checkDir1 = ScmFactory.Directory.getInstance( ws, path );
        Assert.assertEquals( checkDir1.getPath(), path + "/" );
        dir.createSubdirectory( "文件夹b_1198" );
        ScmDirectory checkDir2 = ScmFactory.Directory.getInstance( ws,
                path + "/文件夹b_1198" );
        Assert.assertEquals( checkDir2.getPath(), path + "/文件夹b_1198/" );
        ScmFactory.Directory.deleteInstance( ws, path + "/文件夹b_1198" );
        ScmFactory.Directory.deleteInstance( ws, path );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRlativeDir() throws ScmException {
        String path = "1198";
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, path );
            Assert.fail( "expect fail but act success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreatePathIsNull() throws ScmException {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, null );
            Assert.fail( "expect fail but act success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testPaPathInexist() throws ScmException {
        String paPath = dirBasePath + "/testPaPathInexist";
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                    paPath );
            dir.delete();
            ScmDirectory subdir = dir.createSubdirectory( "testPaPathInexist" );
            Assert.fail( "expect fail but success," + subdir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                throw e;
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testWsIsNull() throws ScmException {
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( null, "a" );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test1() throws ScmException {
        String[] chars = { "//", "\\", "*", "?", "<", ">", "|", "::", "%",
                ";" };
        for ( String c : chars ) {
            try {
                ScmFactory.Directory.createInstance( ws, "1198" + c );
                Assert.fail( "exp fail but act success!!! c = " + c );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                    throw e;
                }
            }
        }

        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws, "." );
            Assert.fail( "expect fail but success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( TestScmBase.forceClear ) {
                ScmFactory.Directory.deleteInstance( ws, dirBasePath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void deleteDir( ScmWorkspace ws, String dirPath )
            throws ScmException {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_NOT_EMPTY ) {
                    throw e;
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
