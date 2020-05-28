package com.sequoiacm.directory.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1184 :: 不同父文件夹中同名的文件并发向相同文件夹中移动
 * @author fanyu
 * @Date:2018年5月2日
 * @version:1.0
 */
public class MoveSameFileToSameDir1184 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/MoveSameFileToSameDir1184";
    private String fullPath1 = dirBasePath + "/dir_1183_a";
    private String fullPath2 = dirBasePath + "/dir_1183_b";
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String author = "MoveSameFileToSameDir1184";
    private ScmDirectory dir1;
    private ScmDirectory dir2;
    private int fileSize = 1024 * 200;
    private File localPath;
    private String filePath;

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
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            deleteDir( ws, fullPath1 );
            dir1 = createDir( ws, fullPath1 );
            dir2 = createDir( ws, fullPath2 );
            createFile( ws, dir1 );
            createFile( ws, dir2 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        MoveFileA maThread = new MoveFileA();
        MoveFileB mbThread = new MoveFileB();
        maThread.start();
        mbThread.start();
        boolean dflag = maThread.isSuccess();
        boolean mflag = mbThread.isSuccess();
        Assert.assertEquals( dflag, true, maThread.getErrorMsg() );
        Assert.assertEquals( mflag, true, mbThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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

    private void createFile( ScmWorkspace ws, ScmDirectory dir )
            throws ScmException {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( author );
            file.setAuthor( author );
            file.setContent( filePath );
            file.setDirectory( dir );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void check( ScmId fileId, ScmDirectory dir ) {
        ScmFile file;
        try {
            file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertEquals( file.getFileName(), author );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getSize(), fileSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( file.getUpdateUser(),
                    TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
            Assert.assertNotNull( file.getUpdateTime() );
            Assert.assertEquals( file.getDirectory().getPath(), dir.getPath() );
            // check results
            SiteWrapper[] expSites = { site };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } catch ( Exception e ) {
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

    private class MoveFileA extends TestThreadBase {
        public void exec() {
            try {
                ScmFile file = ScmFactory.File.getInstance( ws,
                        fileIdList.get( 0 ) );
                ScmDirectory dir = ScmFactory.Directory.getInstance( ws,
                        dirBasePath );
                file.setDirectory( dir );
                check( fileIdList.get( 0 ), dir );
            } catch ( ScmException e ) {
                check( fileIdList.get( 0 ), dir1 );
                if ( e.getError() != ScmError.FILE_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private class MoveFileB extends TestThreadBase {
        public void exec() {
            try {
                ScmFile file = ScmFactory.File.getInstance( ws,
                        fileIdList.get( 1 ) );
                ScmDirectory dir = ScmFactory.Directory.getInstance( ws,
                        dirBasePath );
                file.setDirectory( dir );
                check( fileIdList.get( 1 ), dir );
            } catch ( ScmException e ) {
                check( fileIdList.get( 1 ), dir2 );
                if ( e.getError() != ScmError.FILE_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }
}
