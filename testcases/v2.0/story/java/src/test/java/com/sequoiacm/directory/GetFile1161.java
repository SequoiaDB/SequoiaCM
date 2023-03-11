package com.sequoiacm.directory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1161 :: 获取根文件夹下的文件/获取多级文件夹下的文件
 * @author fanyu
 * @Date:2018年4月25日
 * @version:1.0
 */
public class GetFile1161 extends TestScmBase {
    private boolean runSuccess1;
    private boolean runSuccess2;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/GetFile1161";
    private String fullPath1 = dirBasePath + "/1161_a/1161_c/1161_d";
    private String fullPath2 = dirBasePath + "/1161_b";
    private int fileSize = 1024 * 512 * 1;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String author = "GetFile1161_1";
    private ScmDirectory dir1;
    private ScmDirectory dir2;

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
            dir1 = createDir( ws, fullPath1 );
            dir2 = createDir( ws, fullPath2 );
            // create file
            createFile( ws, ScmFactory.Directory.getInstance( ws, "/" ),
                    author );
            createFile( ws, dir1, author );
            createFile( ws, dir2, author );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    // bug:251
    @Test(groups = { GroupTags.base })
    private void testSubFile() throws Exception {
        try {
            ScmDirectory rootDir = ScmFactory.Directory.getInstance( ws, "/" );
            ScmFile file1 = rootDir.getSubfile( author );
            BSONObject cond1 = new BasicBSONObject();
            cond1.put( "name", author );
            cond1.put( "author", author );
            cond1.put( "filesize", fileSize );
            cond1.put( "title", author );
            cond1.put( "path", rootDir.getPath() );
            cond1.put( "id", fileIdList.get( 0 ) );
            check( file1, cond1 );

            ScmDirectory subDir = ScmFactory.Directory.getInstance( ws,
                    fullPath1 );
            ScmFile file2 = subDir.getSubfile( author );
            BSONObject cond2 = new BasicBSONObject();
            cond2.put( "name", author );
            cond2.put( "author", author );
            cond2.put( "filesize", fileSize );
            cond2.put( "title", author );
            cond2.put( "path", subDir.getPath() );
            cond2.put( "id", fileIdList.get( 1 ) );
            check( file2, cond2 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    // bug:251
    @Test(groups = { GroupTags.base })
    private void testFile1() throws Exception {
        try {
            ScmFile file1 = ScmFactory.File.getInstanceByPath( ws,
                    "/" + author );
            BSONObject cond1 = new BasicBSONObject();
            cond1.put( "name", author );
            cond1.put( "author", author );
            cond1.put( "filesize", fileSize );
            cond1.put( "title", author );
            cond1.put( "path", "/" );
            cond1.put( "id", fileIdList.get( 0 ) );
            check( file1, cond1 );

            ScmDirectory subDir = ScmFactory.Directory.getInstance( ws,
                    fullPath1 );
            String path = fullPath1 + "/" + author;
            ScmFile file2 = ScmFactory.File.getInstanceByPath( ws, path, 1, 0 );
            BSONObject cond2 = new BasicBSONObject();
            cond2.put( "name", author );
            cond2.put( "author", author );
            cond2.put( "filesize", fileSize );
            cond2.put( "title", author );
            cond2.put( "path", subDir.getPath() );
            cond2.put( "id", fileIdList.get( 1 ) );
            System.out.println( "file2String = " + file2.toString() );
            check( file2, cond2 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess2 = true;
    }

    // bug:251
    @Test(groups = { GroupTags.base })
    private void testFile2() throws Exception {
        try {
            String path = fullPath2 + "/" + author;

            ScmFile file1 = ScmFactory.File.getInstanceByPath( ws, path );
            BSONObject cond1 = new BasicBSONObject();
            cond1.put( "name", author );
            cond1.put( "author", author );
            cond1.put( "filesize", fileSize );
            cond1.put( "title", author );
            cond1.put( "path", fullPath2 + "/" );
            cond1.put( "id", fileIdList.get( 2 ) );
            check( file1, cond1 );

            ScmFile file2 = ScmFactory.File.getInstanceByPath( ws, path, 1, 0 );
            BSONObject cond2 = new BasicBSONObject();
            cond2.put( "name", author );
            cond2.put( "author", author );
            cond2.put( "filesize", fileSize );
            cond2.put( "title", author );
            cond2.put( "path", fullPath2 + "/" );
            cond2.put( "id", fileIdList.get( 2 ) );
            check( file2, cond2 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess2 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess1 && runSuccess2 || TestScmBase.forceClear ) {
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

    private void check( ScmFile file, BSONObject cond ) throws Exception {
        try {
            Assert.assertTrue( file.getFileName()
                    .contains( cond.get( "name" ).toString() ) );
            Assert.assertEquals( file.getAuthor(), cond.get( "author" ) );
            Assert.assertEquals( ( int ) file.getSize(),
                    cond.get( "filesize" ) );
            Assert.assertEquals( file.getTitle(), cond.get( "author" ) );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( file.getUpdateUser(),
                    TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime() );
            Assert.assertNotNull( file.getUpdateTime() );
            Assert.assertEquals( file.getFileId(), cond.get( "id" ) );
            Assert.assertEquals( file.getDirectory().getPath(),
                    cond.get( "path" ) );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            SiteWrapper[] expSiteList = { site };
            ScmFileUtils.checkMetaAndData( wsp, file.getFileId(), expSiteList,
                    localPath, filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void createFile( ScmWorkspace ws, ScmDirectory dir,
            String fileName ) {
        ScmFile file;
        try {
            file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            file.setAuthor( author );
            file.setTitle( author );
            file.setDirectory( dir );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
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
        List< String > pathList = new ArrayList<>();
        for ( int i = 1; i < arry.length; i++ ) {
            ele = ele + arry[ i ];
            pathList.add( ele );
            ele = ele + "/";
        }
        return pathList;
    }
}
