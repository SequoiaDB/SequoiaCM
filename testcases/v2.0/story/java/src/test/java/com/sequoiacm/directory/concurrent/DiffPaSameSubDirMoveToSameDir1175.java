package com.sequoiacm.directory.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1175 :: 不同父文件夹中同名的文件夹并发向相同文件夹中移动
 * @author fanyu
 * @Date:2018年5月2日
 * @version:1.0
 */
public class DiffPaSameSubDirMoveToSameDir1175 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/DiffPaSameSubDirMoveToSameDir1175";
    private String fullPath1 = dirBasePath + "/Dir_1175_a";
    private String fullPath2 = dirBasePath + "/Dir_1175_b/Dir_1175_d";
    private String fullPath3 = dirBasePath + "/Dir_1175_c/Dir_1175_d";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            deleteDir( ws, fullPath1 + "/Dir_1175_d" );
            deleteDir( ws, fullPath2 );
            deleteDir( ws, fullPath3 );
            createDir( ws, fullPath1 );
            createDir( ws, fullPath2 );
            createDir( ws, fullPath3 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        MoveDirA maThread = new MoveDirA();
        MoveDirB mbThread = new MoveDirB();
        maThread.start();
        mbThread.start();
        boolean mflag = maThread.isSuccess();
        boolean rflag = mbThread.isSuccess();
        Assert.assertEquals( mflag, true, maThread.getErrorMsg() );
        Assert.assertEquals( rflag, true, mbThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                deleteDir( ws, fullPath1 + "/Dir_1175_d" );
                deleteDir( ws, fullPath2 );
                deleteDir( ws, fullPath3 );
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

    private void checkDestDir( String path, BSONObject expBSON ) {
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, path );
            Assert.assertEquals( dir.getName(), expBSON.get( "name" ) );
            Assert.assertEquals( dir.getPath(), expBSON.get( "path" ) );
            Assert.assertEquals( dir.getUpdateUser(), TestScmBase.scmUserName );
            Assert.assertEquals( dir.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( dir.getWorkspaceName(),
                    expBSON.get( "wsName" ) );
            Assert.assertNotNull( dir.getCreateTime() );
            Assert.assertNotNull( dir.getUpdateTime() );
            Assert.assertEquals( dir.getParentDirectory().getName(),
                    expBSON.get( "paName" ) );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void checkSrcDir( String path ) {
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, path );
            Assert.fail( "expect fail but act success," + dir.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
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
        return ScmFactory.Directory.getInstance( ws,
                pathList.get( pathList.size() - 1 ) );
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

    private class MoveDirA extends TestThreadBase {
        @Override
        public void exec() {
            try {
                String srcPath = fullPath2;
                ScmDirectory destDir = ScmFactory.Directory.getInstance( ws,
                        fullPath1 );
                ScmDirectory srcDir = ScmFactory.Directory.getInstance( ws,
                        srcPath );
                srcDir.move( destDir );
                // check
                BSONObject expBSON1 = new BasicBSONObject();
                expBSON1.put( "name", "Dir_1175_d" );
                expBSON1.put( "path", fullPath1 + "/Dir_1175_d/" );
                expBSON1.put( "wsName", wsp.getName() );
                expBSON1.put( "paName", "Dir_1175_a" );
                checkDestDir( fullPath1 + "/Dir_1175_d/", expBSON1 );
                checkSrcDir( srcPath );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private class MoveDirB extends TestThreadBase {
        @Override
        public void exec() {
            try {
                String srcPath = fullPath3;
                ScmDirectory destDir = ScmFactory.Directory.getInstance( ws,
                        fullPath1 );
                ScmDirectory srcDir = ScmFactory.Directory.getInstance( ws,
                        srcPath );
                srcDir.move( destDir );
                // check
                BSONObject expBSON1 = new BasicBSONObject();
                expBSON1.put( "name", "Dir_1175_d" );
                expBSON1.put( "path", fullPath1 + "/Dir_1175_d/" );
                expBSON1.put( "wsName", wsp.getName() );
                expBSON1.put( "paName", "Dir_1175_a" );
                checkDestDir( fullPath1 + "/Dir_1175_d/", expBSON1 );
                checkSrcDir( srcPath );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }
}
