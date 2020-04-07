package com.sequoiacm.version;

import java.io.File;
import java.io.IOException;
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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description: SCM-2066 :: 文件夹下创建多个版本文件，重命名文件夹后读取文件
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class MutilVersionFileWithDir2066 extends TestScmBase {
    private boolean runSuccess1 = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String name = "MutilVersionFileUnderDir2066";
    private String dirPath = "/2066_A/2066_B/2066_C/2066_D/";
    private String newDirPath = "/2066_F/2066_B/2066_C/2066_D/";
    private ScmDirectory dir = null;
    private int fileSize = 1024;
    private ScmId fileId = null;
    private File localPath = null;
    private List< String > filePathList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        for ( int i = 1; i <= 3; i++ ) {
            String filePath = localPath + File.separator + "localFile_" +
                    ( int ) ( fileSize / Math.pow( 2, i - 1 ) )
                    + ".txt";
            TestTools.LocalFile.createFile( filePath,
                    ( int ) ( fileSize / Math.pow( 2, i - 1 ) ) );
            filePathList.add( filePath );
        }

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        deleteDir( ws, dirPath );
        deleteDir( ws, newDirPath );
        deleteDir( ws, "/2066_A/2066_B/2066_C/2066_F/" );
        dir = createDir( ws, dirPath );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // create file
        fileId = createFile( name, dir, filePathList.get( 0 ) );

        // update file
        updateFile( fileId, filePathList.get( 1 ) );
        updateFile( fileId, filePathList.get( 2 ) );

        // rename dir
        String newname = "2066_F";
        ScmDirectory dir1 = ScmFactory.Directory.getInstance( ws, "/2066_A" );
        dir1.rename( newname );

        String filePath = newDirPath + name;

        // check fileAttr
        checkFileAttr( filePath );

        // check version
        for ( int i = filePathList.size(); i <= 1; i++ ) {
            VersionUtils.CheckFileContentByFile( ws, fileId, i, filePath,
                    localPath );
        }
        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMeta( ws, fileId, expSites );
        runSuccess1 = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !runSuccess1 || TestScmBase.forceClear ) {
                System.out.println( "fileId = " + fileId.get() );
            }
            ScmFactory.File.deleteInstance( ws, fileId, true );
            TestTools.LocalFile.removeFile( localPath );
            deleteDir( ws, dirPath );
            deleteDir( ws, newDirPath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmId createFile( String filename, ScmDirectory dir,
            String filePath ) throws ScmException {
        // upload file and set tags
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( filename );
        file.setAuthor( name );
        file.setDirectory( dir );
        file.setContent( filePath );
        ScmId fileId = file.save();
        return fileId;
    }

    private void updateFile( ScmId fileId, String filePath )
            throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        // file.setTags(tags);
        file.updateContent( filePath );
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
                if ( e.getError() != ScmError.DIR_NOT_FOUND &&
                        e.getError() != ScmError.DIR_NOT_EMPTY ) {
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

    private void checkFileAttr( String filePath ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstanceByPath( ws, filePath );
        Assert.assertEquals( file.getFileName(), name );
        Assert.assertEquals( file.getMajorVersion(), 3 );
        Assert.assertEquals( file.getSize(), fileSize / 4 );
        Assert.assertEquals( file.getDirectory().getPath(), newDirPath );

        // check old dir
        String filePath1 = dirPath + name;
        try {
            ScmFactory.File.getInstanceByPath( ws, filePath1 );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }
}