package com.sequoiacm.directory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1137 :: 创建文件，不指定文件夹/根文件夹/普通文件夹 /指定文件夹不存在
 * @author fanyu
 * @Date:2018年4月23日
 * @version:1.0
 */
public class CreateFileWithSpecifiedDir1137 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/CreateFileWithSpecifiedDir1137";
    private String author = "CreateFileWithSpecifiedDir1137";
    private ScmDirectory scmDir;
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

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
            // ScmFactory.Directory.deleteInstance(ws, dirBasePath);
            deleteDir( ws, dirBasePath );

            scmDir = ScmFactory.Directory.createInstance( ws, dirBasePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() {
        // create file with no specified Dir
        try {
            ScmId fileId = createFileBySpecifiedDir( null );
            check( fileId, ScmFactory.Directory.getInstance( ws, "/" ) );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // create file with specified dir "/"
        try {
            ScmId fileId1 = createFileBySpecifiedDir(
                    ScmFactory.Directory.getInstance( ws, "/" ) );
            check( fileId1, ScmFactory.Directory.getInstance( ws, "/" ) );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // create file with specified normal dir
        try {
            ScmId fileId2 = createFileBySpecifiedDir( scmDir );
            check( fileId2, scmDir );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // create file with specified does not exist dir
        try {
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                    dirBasePath + "/unexist" );
            ScmFactory.Directory.deleteInstance( ws, dirBasePath + "/unexist" );
            createFileBySpecifiedDir( dir );
            Assert.fail( "create file successfully when specified dir does not "
                    + "exist" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                deleteDir( ws, dirBasePath );
                TestTools.LocalFile.removeFile( filePath );
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

    private ScmId createFileBySpecifiedDir( ScmDirectory dir )
            throws ScmException {
        // create file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( author + "_" + UUID.randomUUID() );
        file.setAuthor( author );
        file.setTitle( author );
        file.setMimeType( MimeType.CSS );
        file.setTitle( author );
        if ( dir != null ) {
            file.setDirectory( dir );
        }
        ScmId fileId = file.save();
        fileIdList.add( fileId );
        return fileId;
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
            // Assert.assertEquals(file.getMimeType(),MimeType.PPT);
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
}
