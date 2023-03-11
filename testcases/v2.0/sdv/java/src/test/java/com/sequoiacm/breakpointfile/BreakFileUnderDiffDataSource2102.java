package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
 * @Description: SCM-2102 :: 通过断点文件创建文件和下载文件站点数据源不同
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class BreakFileUnderDiffDataSource2102 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private SiteWrapper sdbSite = null;
    private SiteWrapper hbaseSite = null;
    private SiteWrapper hdfsSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmSession session1 = null;
    private ScmSession session2 = null;
    private ScmWorkspace ws = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;

    private String name = "BreakFileUnderDiffDataSource2102";
    private int fileSize = 1024;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
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
                    .equals( DatasourceType.HBASE ) ) {
                hbaseSite = siteList.get( i );
            }

            if ( siteList.get( i ).getDataType()
                    .equals( DatasourceType.HDFS ) ) {
                hdfsSite = siteList.get( i );
            }

            if ( siteList.get( i ).getDataType()
                    .equals( DatasourceType.SEQUOIADB ) ) {
                sdbSite = siteList.get( i );
            }
        }
        if ( hbaseSite == null || hdfsSite == null || sdbSite == null ) {
            throw new SkipTestException( "NO Datasourse, Skip!" );
        }

        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( hbaseSite );
        session1 = TestScmTools.createSession( hdfsSite );
        session2 = TestScmTools.createSession( sdbSite );

        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session2 );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateFileInHbase() throws Exception {
        ScmBreakpointFile breakpointFile = createBreakpointFile( ws2, name,
                filePath );
        ScmId fileId = breakpointFile2ScmFile( ws2, breakpointFile, name );
        int currentVerion = 1;
        checkResult( fileId, ws, currentVerion );
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateFileInHdfs() throws Exception {
        ScmBreakpointFile breakpointFile = createBreakpointFile( ws2, name,
                filePath );
        ScmId fileId = breakpointFile2ScmFile( ws2, breakpointFile, name );
        int currentVerion = 1;
        checkResult( fileId, ws1, currentVerion );
        runSuccess2 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testUnSupport() throws Exception {
        try {
            createBreakpointFile( ws, name, filePath );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        try {
            createBreakpointFile( ws1, name, filePath );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess2 = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !runSuccess1 || !runSuccess2 || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    System.out.println( "fileId = " + fileId.get() );
                }
            }
            for ( ScmId fileId : fileIdList ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( session1 != null ) {
                session1.close();
            }
            if ( session2 != null ) {
                session2.close();
            }
        }
    }

    private ScmBreakpointFile createBreakpointFile( ScmWorkspace ws,
            String name, String filePath ) throws ScmException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, name + "_" + UUID.randomUUID() );
        breakpointFile.upload( new File( filePath ) );
        return breakpointFile;
    }

    private ScmId breakpointFile2ScmFile( ScmWorkspace ws,
            ScmBreakpointFile breakpointFile, String name )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setAuthor( name );
        ScmId fileId = file.save();
        fileIdList.add( fileId );
        return fileId;
    }

    private void checkResult( ScmId fileId, ScmWorkspace ws, int currentVerion )
            throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getAuthor(), name );
        Assert.assertEquals( file.getMajorVersion(), currentVerion );
        Assert.assertEquals( file.getSize(), fileSize );
        Assert.assertEquals( file.getDirectory().getPath(), "/" );
        // check content
        VersionUtils.CheckFileContentByFile( ws, fileId, currentVerion,
                filePath, localPath );
    }
}