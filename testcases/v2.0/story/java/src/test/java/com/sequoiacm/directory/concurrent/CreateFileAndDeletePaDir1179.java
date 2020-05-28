package com.sequoiacm.directory.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

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
import com.sequoiacm.common.MimeType;
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
 * @Description:SCM-1179 :: 创建文件和删除其父文件夹并发
 * @author fanyu
 * @Date:2018年5月2日
 * @version:1.0
 */
public class CreateFileAndDeletePaDir1179 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/CreateFileAndDeletePaDir1179";
    private String fullPath1 = dirBasePath;
    private String author = "CreateFileAndDeletePaDir1179";
    private ScmDirectory dir;
    private List< ScmId > fileIdList = new CopyOnWriteArrayList< ScmId >();
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
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            deleteDir( ws, fullPath1 );
            dir = ScmFactory.Directory.createInstance( ws, fullPath1 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        DeletePaDir dThread = new DeletePaDir();
        CreateFile cThread = new CreateFile();
        dThread.start();
        cThread.start();
        boolean dflag = dThread.isSuccess();
        boolean cflag = cThread.isSuccess();
        Assert.assertEquals( dflag, true, dThread.getErrorMsg() );
        Assert.assertEquals( cflag, true, cThread.getErrorMsg() );
        // check
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

    private void check( ScmId fileId, ScmDirectory dir ) {
        ScmFile file;
        try {
            file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertNotNull( file.getFileName() );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getTitle(), author );
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

    private void checkDir( String path ) {
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

    private class DeletePaDir extends TestThreadBase {
        @Override
        public void exec() {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.Directory.deleteInstance( ws, fullPath1 );
                checkDir( fullPath1 );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_EMPTY ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CreateFile extends TestThreadBase {
        public void exec() {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( author + "_" + UUID.randomUUID() );
                file.setAuthor( author );
                file.setTitle( author );
                file.setMimeType( MimeType.GIF );
                file.setTitle( author );
                file.setDirectory( dir );
                ScmId fileId = file.save();
                check( fileId, dir );
                fileIdList.add( fileId );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
