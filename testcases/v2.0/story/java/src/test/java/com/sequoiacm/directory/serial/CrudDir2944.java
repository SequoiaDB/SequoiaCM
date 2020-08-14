package com.sequoiacm.directory.serial;

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
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-2944:并发创建/重命名/移动/获取/删除相同目录和不同目录
 * @author fanyu
 * @Date:2020年06月28日
 * @version:1.0
 */
public class CrudDir2944 extends TestScmBase {
    private SiteWrapper site;
    private String[] dirPaths = { "/dir2944a1/b1/c1/d1/e1",
            "/dir2944a2/b2/c2/d2/e2", "/dir2944a3/b3/c3/d3/e3",
            "/dir2944a4/b4/c4/d4/e4", "/dir2944a5/b5/c5/d5/e5",
            "/dir2944a6/b6/c6/d6/e6", "/dir2944a7" };
    private ScmSession session;
    private WsWrapper wsp;
    private ScmWorkspace ws;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理环境
        for ( String dirPath : dirPaths ) {
            while ( !dirPath.equals( "" ) ) {
                try {
                    ScmFactory.Directory.deleteInstance( ws, dirPath );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.DIR_NOT_FOUND
                            && e.getError() != ScmError.DIR_NOT_EMPTY ) {
                        e.printStackTrace();
                        throw e;
                    }
                }
                dirPath = dirPath.substring( 0, dirPath.lastIndexOf( "/" ) );
            }
        }

        // 创建目录，为后面移动/重命名/获取/删除做准备
        for ( String dirPath : dirPaths ) {
            String subDirPath = "/";
            while ( dirPath.indexOf( "/", subDirPath.length() ) != -1 ) {
                subDirPath = subDirPath
                        + dirPath.substring( subDirPath.length(),
                                dirPath.indexOf( "/", subDirPath.length() ) );
                subDirPath = subDirPath + "/";
                ScmFactory.Directory.createInstance( ws, subDirPath );
            }
            ScmFactory.Directory.createInstance( ws, dirPath );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        // 创建目录
        for ( int i = 0; i < 10; i++ ) {
            // 不同目录
            threadExec.addWorker( new CreateDir( "/dir2944a1/" + i ) );
            // 相同目录
            threadExec.addWorker( new CreateDir( "/dir2944a1/" + i ) );
        }

        // 重命名目录
        threadExec.addWorker( new RenameDir( dirPaths[ 0 ], "e1new" ) );
        // 相同目录
        threadExec.addWorker( new RenameDir( dirPaths[ 0 ], "e1new" ) );
        // 不同目录
        threadExec.addWorker( new RenameDir( dirPaths[ 1 ], "e2new" ) );

        // 移动目录
        threadExec.addWorker(
                new MoveDir( "/dir2944a3/b3/c3/d3/e3", "/dir2944a3" ) );
        // 相同目录
        threadExec.addWorker(
                new MoveDir( "/dir2944a3/b3/c3/d3/e3", "/dir2944a3" ) );
        // 不同目录
        threadExec.addWorker(
                new MoveDir( "/dir2944a4/b4/c4/d4/e4", "/dir2944a4" ) );

        // 获取目录
        String getDirPath = "/";
        while ( dirPaths[ 4 ].indexOf( "/", getDirPath.length() ) != -1 ) {
            getDirPath = getDirPath
                    + dirPaths[ 4 ].substring( getDirPath.length(),
                            dirPaths[ 4 ].indexOf( "/", getDirPath.length() ) );
            threadExec.addWorker( new GetDir( getDirPath ) );
            threadExec.addWorker( new GetDir( getDirPath ) );
            getDirPath = getDirPath + "/";
        }

        // 删除目录
        threadExec.addWorker( new DeleteDir( dirPaths[ 5 ] ) );
        // 相同目录
        threadExec.addWorker( new DeleteDir( dirPaths[ 5 ] ) );
        // 不同目录
        threadExec.addWorker( new DeleteDir( dirPaths[ 6 ] ) );
        threadExec.run();

        // 最终预期的目录
        String[] expDirPaths = { "/dir2944a1/0", "/dir2944a1/1", "/dir2944a1/2",
                "/dir2944a1/3", "/dir2944a1/4", "/dir2944a1/5", "/dir2944a1/6",
                "/dir2944a1/7", "/dir2944a1/8", "/dir2944a1/9",
                "/dir2944a1/b1/c1/d1/e1new", "/dir2944a2/b2/c2/d2/e2new",
                "/dir2944a3/e3", "/dir2944a3/b3/c3/d3", "/dir2944a4/e4",
                "/dir2944a4/b4/c4/d4", "/dir2944a5/b5/c5/d5/e5",
                "/dir2944a6/b6/c6/d6" };
        // 检查最终结果
        for ( String path : expDirPaths ) {
            Assert.assertEquals(
                    ScmFactory.Directory.isInstanceExist( ws, path ), true,
                    path );
        }

        // 删除目录
        for ( String dirPath : expDirPaths ) {
            while ( !dirPath.equals( "" ) ) {
                try {
                    ScmFactory.Directory.deleteInstance( ws, dirPath );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.DIR_NOT_EMPTY ) {
                        throw e;
                    }
                }
                dirPath = dirPath.substring( 0, dirPath.lastIndexOf( "/" ) );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        session.close();
    }

    private class CreateDir {
        String path;

        public CreateDir( String path ) {
            this.path = path;
        }

        @ExecuteOrder(step = 1)
        private void createDir() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.Directory.createInstance( ws, path );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_EXIST ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class RenameDir {
        private String renamePath;
        private String newName;

        public RenameDir( String renamePath, String newName ) {
            this.renamePath = renamePath;
            this.newName = newName;
        }

        @ExecuteOrder(step = 1)
        private void renameDir() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmDirectory renameDir = ScmFactory.Directory.getInstance( ws,
                        renamePath );
                renameDir.rename( newName );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class MoveDir {
        private String srcPath;
        private String destPath;

        public MoveDir( String srcPath, String destPath ) {
            this.srcPath = srcPath;
            this.destPath = destPath;
        }

        @ExecuteOrder(step = 1)
        private void moveDir() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmDirectory srcDir = ScmFactory.Directory.getInstance( ws,
                        srcPath );
                ScmDirectory destDir = ScmFactory.Directory.getInstance( ws,
                        destPath );
                srcDir.move( destDir );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class GetDir {
        private String dirPath;

        public GetDir( String dirPath ) {
            this.dirPath = dirPath;
        }

        @ExecuteOrder(step = 1)
        private void getDir() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.Directory.getInstance( ws, dirPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteDir {
        private String dirPath;

        public DeleteDir( String dirPath ) {
            this.dirPath = dirPath;
        }

        @ExecuteOrder(step = 1)
        private void deleteDir() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.Directory.deleteInstance( ws, dirPath );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
