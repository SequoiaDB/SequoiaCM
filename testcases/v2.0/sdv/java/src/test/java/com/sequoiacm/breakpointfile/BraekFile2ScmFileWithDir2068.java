package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testresource.SkipTestException;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
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
 * @Description: SCM-2068 :: 通过断点文件创建文件时指定文件夹
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class BraekFile2ScmFileWithDir2068 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String name = "BraekFile2ScmFileUnderDir2068";
    private int fileSize = 1024;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String dirPath = "/2066_A/2066_B/2066_C/2066_D/";

    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< SiteWrapper > siteList = ScmInfo.getAllSites();
        for ( int i = 0; i < siteList.size(); i++ ) {
            if ( siteList.get( i ).getDataType()
                    .equals( DatasourceType.SEQUOIADB ) ) {
                site = siteList.get( i );
                break;
            }
            if ( i == siteList.size() - 1 ) {
                throw new SkipTestException( "NO Sequoiadb Datasourse, Skip!" );
            }
        }
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        deleteDir( ws, dirPath );
        createDir( ws, dirPath );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testDirHasSame() throws Exception {
        // create file in dir
        ScmDirectory dir = ScmFactory.Directory.getInstance( ws, dirPath );
        createFile( null, filePath, name + "_1", dir );

        // breakpointFile transfer to ScmFile
        ScmBreakpointFile breakpointFile = createBreakpointFile( name + "_1",
                filePath );

        try {
            createFile( breakpointFile, null, name + "_1", dir );
            Assert.fail( "expect fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        // check,breakpointFile is still exist
        ScmBreakpointFile file = ScmFactory.BreakpointFile.getInstance( ws,
                name + "_1" );
        Assert.assertEquals( file.getFileName(), name + "_1" );
        Assert.assertEquals( file.getUploadSize(), fileSize );
        ScmFactory.BreakpointFile.deleteInstance( ws, name + "_1" );
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testOtherDirHasSame() throws Exception {
        // create file in dir
        ScmDirectory dir = ScmFactory.Directory.getInstance( ws,
                "/2066_A/2066_B/2066_C" );
        createFile( null, filePath, name + "_2", dir );

        // breakpointFile transfer to ScmFile
        ScmBreakpointFile breakpointFile = createBreakpointFile( name + "_2",
                filePath );
        ScmDirectory dir1 = ScmFactory.Directory.getInstance( ws, dirPath );
        ScmId fileId = createFile( breakpointFile, null, name + "_2", dir1 );

        // check,breakpointFile does not exist
        try {
            ScmFactory.BreakpointFile.getInstance( ws, name + "_2" );
            Assert.fail( "expect fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        // check file fullpath is right
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getFileName(), name + "_2" );
        Assert.assertEquals( file.getDirectory().getPath(), dirPath );
        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        runSuccess2 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // breakpointFile transfer to ScmFile
        ScmBreakpointFile breakpointFile = createBreakpointFile( name + "_3",
                filePath );
        ScmDirectory dir1 = ScmFactory.Directory.getInstance( ws, dirPath );
        ScmId fileId = createFile( breakpointFile, null, name + "_3", dir1 );

        // check,breakpointFile does not exist
        try {
            ScmFactory.BreakpointFile.getInstance( ws, name + "_3" );
            Assert.fail( "expect fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        // check file fullpath is right
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getFileName(), name + "_3" );
        Assert.assertEquals( file.getDirectory().getPath(), dirPath );
        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        runSuccess3 = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !runSuccess1 || !runSuccess2 || !runSuccess3
                    || TestScmBase.forceClear ) {
                if ( fileIdList != null ) {
                    for ( ScmId fileId : fileIdList ) {
                        System.out.println( "fileId = " + fileId.get() );
                    }
                }
            }
            for ( ScmId fileId : fileIdList ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            deleteDir( ws, dirPath );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmBreakpointFile createBreakpointFile( String name,
            String filePath ) throws ScmException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, name );
        breakpointFile.upload( new File( filePath ) );
        return breakpointFile;
    }

    private ScmId createFile( ScmBreakpointFile breakpointFile, String filePath,
            String name, ScmDirectory dir ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        if ( breakpointFile != null ) {
            file.setContent( breakpointFile );
        }
        if ( filePath != null ) {
            file.setContent( filePath );
        }
        file.setFileName( name );
        file.setAuthor( name );
        file.setDirectory( dir );
        ScmId fileId = file.save();
        fileIdList.add( fileId );
        return fileId;
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
}
