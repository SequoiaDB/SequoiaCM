package com.sequoiacm.directory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1132 :: 创建文件夹，检查文件夹的各个属性值
 * @author fanyu
 * @Date:2018年4月20日
 * @version:1.0
 */
public class CreateDir1132 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private String dirName = "CreateDir1132";
    private String subDirName = "CreateDir1132_1";
    private int subDirNum = 1;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String author = "CreateDir1132";
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileSize = 1;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 1;

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
            deleteDir( ws, "/" + dirName );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                "/" + dirName );
        ScmDirectory subDir = dir.createSubdirectory( subDirName );
        write( ws, dir );
        write( ws, subDir );
        check( ws, "/" + dirName );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                deleteDir( ws, "/" + dirName + "/" + subDirName );
                TestTools.LocalFile.removeFile( localPath );
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

    private void write( ScmWorkspace ws, ScmDirectory scmDir ) {
        try {
            // create file
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( author + "_" + scmDir.getName() );
            file.setAuthor( author );
            file.setMimeType( MimeType.JPEG );
            file.setTitle( author );
            file.setDirectory( scmDir );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void check( ScmWorkspace ws, String dirPath ) throws Exception {
        try {
            ScmDirectory scmDir = ScmFactory.Directory.getInstance( ws,
                    dirPath );
            Assert.assertEquals(
                    scmDir.getSubfile( author + "_" + scmDir.getName() )
                            .getFileName(),
                    author + "_" + scmDir.getName() );
            checkDirAttr( scmDir, '/' + dirName + "/", dirName, "/" );

            ScmCursor< ScmFileBasicInfo > fileCursor = scmDir.listFiles(
                    new BasicBSONObject( ScmAttributeName.File.FILE_NAME,
                            author + "_" + scmDir.getName() ) );
            checkFileCursorAttr( fileCursor );

            ScmDirectory subDir = scmDir.getSubdirectory( subDirName );
            Assert.assertNotNull(
                    subDir.getSubfile( author + "_" + subDir.getName() ) );
            checkDirAttr( subDir, '/' + dirName + "/" + subDirName + "/",
                    subDirName, dirName );

            ScmCursor< ScmDirectory > dirCursor = scmDir.listDirectories(
                    new BasicBSONObject( ScmAttributeName.Directory.NAME,
                            subDirName ) );
            checkDirCurSorAttr( dirCursor );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void checkDirAttr( ScmDirectory scmDir, String path, String dirName,
            String paName ) {
        try {
            Assert.assertEquals( scmDir.getName(), dirName );
            Assert.assertEquals( scmDir.getParentDirectory().getName(),
                    paName );
            Assert.assertEquals( scmDir.getPath(), path );
            Assert.assertEquals( scmDir.getUpdateUser(),
                    TestScmBase.scmUserName );
            Assert.assertEquals( scmDir.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( scmDir.getWorkspaceName(), wsp.getName() );
            Assert.assertNotNull( scmDir.getCreateTime() );
            Assert.assertNotNull( scmDir.getCreateTime() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void checkFileCursorAttr( ScmCursor< ScmFileBasicInfo > fileCursor )
            throws Exception {
        int i = 0;
        try {
            while ( fileCursor.hasNext() ) {
                ScmFileBasicInfo fileInfo = fileCursor.getNext();
                Assert.assertNotNull( fileInfo.getFileName() );
                Assert.assertEquals( fileInfo.getMajorVersion(), 1 );
                Assert.assertEquals( fileInfo.getMinorVersion(), 0 );
                Assert.assertNotNull( fileInfo.getFileId() );

                // check content
                SiteWrapper[] expSites = { site };
                ScmFileUtils.checkMetaAndData( wsp, fileInfo.getFileId(),
                        expSites, localPath, filePath );
                i++;
            }
            Assert.assertEquals( i, fileNum,
                    "the dir has more than expected file's num" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void checkDirCurSorAttr( ScmCursor< ScmDirectory > dirCursor ) {
        int i = 0;
        try {
            while ( dirCursor.hasNext() ) {
                ScmDirectory dirInfo = dirCursor.getNext();
                Assert.assertEquals( dirInfo.getName(), subDirName );
                Assert.assertEquals( dirInfo.getPath(),
                        "/" + dirName + "/" + subDirName + "/" );
                Assert.assertEquals( dirInfo.getWorkspaceName(),
                        wsp.getName() );
                i++;
            }
            Assert.assertEquals( i, subDirNum,
                    "the dir has more than expected subdir's num" );
        } catch ( ScmException e ) {
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
}
