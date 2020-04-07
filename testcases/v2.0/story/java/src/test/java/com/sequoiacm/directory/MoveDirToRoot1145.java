package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
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
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1145 :: 文件夹移动到根文件夹
 * @author fanyu
 * @Date:2018年4月24日
 * @version:1.0
 */
public class MoveDirToRoot1145 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/MoveDirToRoot1145";
    private String fullPath = dirBasePath + "/1145_b/1145_c";
    private String eleName = "test_1145";
    private ScmDirectory dir;
    private String author = "MoveDirToRoot1145";
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            dir = createDir( ws, fullPath );
            createSubDirAndFile( ws, dir );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            // eg:/a/b/c mv c to /
            dir.move( ScmFactory.Directory.getInstance( ws, "/" ) );
            // check sub path
            check( fileId, dir, ws );
            dir.createSubdirectory( eleName + "_test" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Directory
                        .deleteInstance( ws, dirBasePath + "/1145_b" );
                ScmFactory.Directory.deleteInstance( ws, dirBasePath );
                ScmFactory.Directory.deleteInstance( ws, "/1145_c/" + eleName );
                ScmFactory.Directory
                        .deleteInstance( ws, "/1145_c/" + eleName + "_test" );
                ScmFactory.Directory.deleteInstance( ws, "/1145_c" );
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

    private void check( ScmId fileId, ScmDirectory dir, ScmWorkspace ws ) {
        ScmFile file;
        try {
            file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getDirectory().getName(), dir.getName() );
            Assert.assertEquals( dir.getSubdirectory( eleName ).getName(),
                    eleName );
            Assert.assertEquals( dir.getParentDirectory().getPath(), "/" );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void createSubDirAndFile( ScmWorkspace ws, ScmDirectory dir )
            throws ScmException {
        try {
            dir.createSubdirectory( eleName );
            createFile( ws, dir );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void createFile( ScmWorkspace ws, ScmDirectory dir ) {
        ScmFile file;
        try {
            file = ScmFactory.File.createInstance( ws );
            file.setFileName( author + "_" + UUID.randomUUID() );
            file.setAuthor( author );
            file.setDirectory( dir );
            fileId = file.save();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private ScmDirectory createDir( ScmWorkspace ws, String dirPath )
            throws ScmException {
        List< String > pathList = getSubPaths( dirPath );
        for ( String path : pathList ) {
            try {
                ScmFactory.Directory.createInstance( ws, path );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
        return ScmFactory.Directory
                .getInstance( ws, pathList.get( pathList.size() - 1 ) );
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