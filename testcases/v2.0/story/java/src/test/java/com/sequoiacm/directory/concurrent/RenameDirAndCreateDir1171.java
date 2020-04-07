package com.sequoiacm.directory.concurrent;

import java.io.File;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1171 :: 修改文件夹名和创建同名文件夹并发
 * @author fanyu
 * @Date:2018年4月27日
 * @version:1.0
 */
public class RenameDirAndCreateDir1171 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/RenameDirAndCreateDir1171";
    private String fullPath1 = dirBasePath + "/Dir_1171";
    private String fullPath2 = dirBasePath + "/Dir_New_1171";
    private String newName = "Dir_New_1171";
    private String author = "RenameDirAndCreateDir1171";
    private int fileSize = 10 * 1024;
    private File localPath;
    private String filePath;
    private ScmId fileId;
    private ScmDirectory dir;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            deleteDir( ws, fullPath2 );
            dir = createDir( ws, fullPath1 );
            createFile( ws, dir );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ReNameDir rThread = new ReNameDir();
        CreateDir cThread = new CreateDir();
        rThread.start();
        cThread.start();
        boolean rflag = rThread.isSuccess();
        boolean cflag = cThread.isSuccess();
        Assert.assertEquals( rflag, true, rThread.getErrorMsg() );
        Assert.assertEquals( cflag, true, cThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                deleteDir( ws, fullPath1 );
                deleteDir( ws, fullPath2 );
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

    private void checkDir( String path, BSONObject expBSON ) {
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

    private void checkFile( String path ) {
        try {
            //ScmFile file = ScmFactory.File.getInstance(ws, path);
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getDirectory().getPath(),
                    fullPath2 + "/" );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getFileName(), author );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getSize(), fileSize );
            Assert.assertEquals( file.getTitle(), author );
            Assert.assertEquals( file.getUpdateUser(),
                    TestScmBase.scmUserName );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertNotNull( file.getUpdateTime() );
            Assert.assertNotNull( file.getCreateTime() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void createFile( ScmWorkspace ws, ScmDirectory dir ) {
        ScmFile file;
        try {
            file = ScmFactory.File.createInstance( ws );
            file.setFileName( author );
            file.setAuthor( author );
            file.setTitle( author );
            file.setContent( filePath );
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

    private class ReNameDir extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmDirectory dir = ScmFactory.Directory
                        .getInstance( ws, fullPath1 );
                dir.rename( newName );
                // check
                BSONObject expBSON1 = new BasicBSONObject();
                expBSON1.put( "name", newName );
                expBSON1.put( "path", fullPath2 + "/" );
                expBSON1.put( "wsName", wsp.getName() );
                expBSON1.put( "paName", "RenameDirAndCreateDir1171" );
                checkDir( fullPath2, expBSON1 );

                //check
                checkFile( fullPath2 + "/" + author );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private class CreateDir extends TestThreadBase {
        public void exec() {
            try {
                ScmFactory.Directory.createInstance( ws, fullPath1 );
                // check
                BSONObject expBSON = new BasicBSONObject();
                expBSON.put( "name", "Dir_1171" );
                expBSON.put( "path", fullPath1 + "/" );
                expBSON.put( "wsName", wsp.getName() );
                expBSON.put( "paName", "RenameDirAndCreateDir1171" );
                checkDir( fullPath1, expBSON );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }
}
