/**
 *
 */
package com.sequoiacm.directory;

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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1143 :: 重命名文件夹，同级下重名
 * @author fanyu
 * @Date:2018年4月24日
 * @version:1.0
 */
public class RenameDirOnSameName1143 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/RenameDirOnSameName1143";
    private String eleName = "file_1143";
    private String eleName1 = "subdir_1143";
    private String eleName2 = "subdir_test_1143";
    private int fileSize = 1024 * 400;
    private File localPath;
    private String filePath;
    private ScmId fileId;
    private ScmDirectory scmdir;
    private String newName;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( eleName ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            deleteDir( ws, dirBasePath + "/" + eleName1 );
            deleteDir( ws, dirBasePath + "/" + newName );

            scmdir = ScmFactory.Directory.createInstance( ws, dirBasePath );
            createDirAndFile( ws, scmdir );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // create test dir
        ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                dirBasePath + "/" + eleName2 );
        // rename test dir when parent's dir has same name file
        try {
            dir.rename( eleName );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        // rename test dir when parent's dir has same name dir
        try {
            dir.rename( eleName1 );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        // check result
        newName = eleName2;
        check( fileId, dir, ws );

        // dir can rename agagin
        newName = eleName2 + "_2";
        dir.rename( newName );
        check( fileId, dir, ws );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Directory.deleteInstance( ws,
                        dirBasePath + "/" + eleName1 );
                ScmFactory.Directory.deleteInstance( ws,
                        dirBasePath + "/" + newName );
                ScmFactory.Directory.deleteInstance( ws, dirBasePath );
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

    private void createDirAndFile( ScmWorkspace ws, ScmDirectory dir )
            throws ScmException {
        try {
            ScmFactory.Directory.createInstance( ws,
                    dirBasePath + "/" + eleName1 );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( eleName );
            file.setAuthor( eleName );
            file.setDirectory( dir );
            file.setContent( filePath );
            fileId = file.save();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void check( ScmId fileId, ScmDirectory dir, ScmWorkspace ws ) {
        ScmFile file;
        try {
            file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getWorkspaceName(), ws.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertNotNull( file.getFileName() );
            Assert.assertEquals( file.getAuthor(), eleName );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( file.getUpdateUser(),
                    TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime() );
            Assert.assertNotNull( file.getUpdateTime() );

            Assert.assertEquals( file.getDirectory().getPath(),
                    dirBasePath + "/" );
            Assert.assertEquals( dir.getName(), newName );
            Assert.assertEquals( dir.getUpdateUser(), TestScmBase.scmUserName );
            Assert.assertEquals( dir.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( dir.getParentDirectory().getPath(),
                    dirBasePath + "/" );
            Assert.assertNotNull( dir.getCreateTime() );
            Assert.assertNotNull( dir.getUpdateTime() );

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
