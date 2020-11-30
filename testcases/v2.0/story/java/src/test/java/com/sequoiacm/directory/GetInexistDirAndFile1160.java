package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1160 :: 获取不存在的文件夹/获取不存在的文件
 * @author fanyu
 * @Date:2018年4月26日
 * @version:1.0
 */
public class GetInexistDirAndFile1160 extends TestScmBase {
    private boolean runSuccess1;
    private boolean runSuccess2;
    private boolean runSuccess3;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/GetInexistDirAndFile1160";
    private String fullPath1 = dirBasePath + "/1160_a/1160_b";
    private ScmDirectory dir;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        deleteDir( ws, fullPath1 );
        dir = createDir( ws, fullPath1 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void getInexistDirtest() throws ScmException {
        try {
            ScmFactory.Directory.getInstance( ws, "/1160_a/测试1160" );
            Assert.fail( "expect fail but act successfully" );
        } catch ( ScmException e ) {
            Assert.assertTrue( e.getMessage().contains( "测试1160" ) );
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                throw e;
            }
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void getPathExistTest() throws ScmException {
        ScmFile file1 = ScmFactory.File.createInstance( ws );
        String fileName = "GetInexistDirAndFile1160_getPathExisttest";
        file1.setFileName( fileName );
        file1.setDirectory( dir );
        ScmId fileId = file1.save();
        ScmFactory.File.deleteInstance( ws, fileId, true );
        ScmFile file2 = dir.getSubfile( fileName );
        Assert.assertNull( file2 );
        runSuccess2 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void getFileExistTest() throws ScmException {
        ScmId fileId = null;
        try {
            ScmFile file1 = ScmFactory.File.createInstance( ws );
            String fileName = "GetInexistDirAndFile1160_getFileExisttest";
            file1.setFileName( fileName );
            file1.setDirectory( ScmFactory.Directory.getInstance( ws, "/" ) );
            fileId = file1.save();
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                    "/1160_a" );
            dir.delete();
            ScmFile file2 = dir.getSubfile( fileName );
            Assert.assertNull( file2 );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        }
        runSuccess3 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess1 && runSuccess2 && runSuccess3
                    || TestScmBase.forceClear ) {
                deleteDir( ws, fullPath1 );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmDirectory createDir( ScmWorkspace ws, String dirPath )
            throws ScmException {
        List< String > pathList = getSubPaths( dirPath );
        for ( String path : pathList ) {
            try {
                ScmFactory.Directory.createInstance( ws, path );
            } catch ( ScmException e ) {
                dir = ScmFactory.Directory.getInstance( ws, path );
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
