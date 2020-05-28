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
 * @Description:SCM-1180 :: 修改文件名和创建同名文件夹并发
 * @author fanyu
 * @Date:2018年5月2日
 * @version:1.0
 */
public class ReNameFileAndCreateSameNameDir1180 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/ReNameFileAndCreateSameNameDir1180";
    private String fullPath1 = dirBasePath;
    private String fullPath2 = dirBasePath
            + "/ReNameFileAndCreateSameNameDir1180";
    private ScmId fileId;
    private String author = "ReNameFileAndCreateSameNameDir1180";
    private ScmDirectory dir;
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
            deleteDir( ws, fullPath2 );
            dir = ScmFactory.Directory.createInstance( ws, fullPath1 );
            createFile( ws, dir );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        CreateDir cThread = new CreateDir();
        ReNameFile rThread = new ReNameFile();
        cThread.start();
        rThread.start();
        boolean cflag = cThread.isSuccess();
        boolean rflag = rThread.isSuccess();
        Assert.assertEquals( cflag, true, cThread.getErrorMsg() );
        Assert.assertEquals( rflag, true, rThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
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

    private void check( String path, BSONObject expBSON ) {
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

    private void createFile( ScmWorkspace ws, ScmDirectory dir )
            throws ScmException {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( author );
            file.setAuthor( author );
            file.setContent( filePath );
            file.setDirectory( dir );
            fileId = file.save();
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
            Assert.assertEquals( file.getFileName(), author + "_new" );
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

    private class CreateDir extends TestThreadBase {
        public void exec() {
            try {
                ScmFactory.Directory.createInstance( ws, fullPath2 );
                // check
                BSONObject expBSON = new BasicBSONObject();
                expBSON.put( "name", "ReNameFileAndCreateSameNameDir1180" );
                expBSON.put( "path", fullPath2 + "/" );
                expBSON.put( "wsName", wsp.getName() );
                expBSON.put( "paName", "ReNameFileAndCreateSameNameDir1180" );
                check( fullPath2, expBSON );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private class ReNameFile extends TestThreadBase {
        public void exec() {
            try {
                String newFileName = author + "_new";
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setFileName( newFileName );
                check( fileId, dir );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }
}
