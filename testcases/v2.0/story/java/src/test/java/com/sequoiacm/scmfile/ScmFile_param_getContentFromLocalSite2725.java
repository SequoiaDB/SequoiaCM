package com.sequoiacm.scmfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2725:getContentFromLocalSite参数校验
 * @author fanyu
 * @Date:2019年12月03日
 * @version:1.0
 */

public class ScmFile_param_getContentFromLocalSite2725 extends TestScmBase {
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private WsWrapper wsp;
    private ScmSession session1;
    private ScmSession session2;
    private AtomicInteger actTestSuccessCount = new AtomicInteger( 0 );
    private int expTestSuccessCount = 4;
    private ScmWorkspace ws;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String name = "file2725";
    private File localPath;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        List< SiteWrapper > branchSites = ScmInfo.getBranchSites( 2 );
        branchSite1 = branchSites.get( 0 );
        branchSite2 = branchSites.get( 1 );
        wsp = ScmInfo.getWs();
        session1 = ScmSessionUtils.createSession( branchSite1 );
        session2 = ScmSessionUtils.createSession( branchSite2 );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );
        // prepare scm file
        Calendar cal = Calendar.getInstance();
        createScmFile( session1, cal.getTime() );
        createScmFile( session2, cal.getTime() );
        cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - 1 );
        createScmFile( session1, cal.getTime() );
    }

    @Test(groups = { "fourSite" })
    private void testPathIsEmptyStr() throws ScmException {
        ScmFile scmFile = ScmFactory.File.getInstance( ws,
                fileIdList.get( 0 ) );
        try {
            scmFile.getContentFromLocalSite( "" );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        actTestSuccessCount.getAndIncrement();
    }

    @Test(groups = { "fourSite" })
    private void testPathIsDir() throws ScmException {
        ScmFile scmFile = ScmFactory.File.getInstance( ws,
                fileIdList.get( 0 ) );
        try {
            scmFile.getContentFromLocalSite( localPath.toString() );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_IS_DIRECTORY ) {
                throw e;
            }
        }
        actTestSuccessCount.getAndIncrement();
    }

    @Test(groups = { "fourSite" })
    private void testPathNotExist() throws ScmException {
        ScmFile scmFile = ScmFactory.File.getInstance( ws,
                fileIdList.get( 0 ) );
        try {
            scmFile.getContentFromLocalSite( localPath + "/test/test" );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_IO ) {
                throw e;
            }
        }
        actTestSuccessCount.getAndIncrement();
    }

    @Test(groups = { "fourSite" })
    private void testDataNotExist() throws ScmException, IOException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                session2 );
        ScmFile scmFile1 = ScmFactory.File.getInstance( ws,
                fileIdList.get( 0 ) );
        ScmFile scmFile2 = ScmFactory.File.getInstance( ws,
                fileIdList.get( 2 ) );
        String downloadPath = localPath + File.separator + "testDataNotExist1";
        try {
            // scmfile1：site2存在对应的LOB表不存在数据
            scmFile1.getContentFromLocalSite( downloadPath );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DATA_NOT_EXIST ) {
                throw e;
            }
        }
        try {
            // scmfile2：site2不存在对应的LOB表
            scmFile2.getContentFromLocalSite(
                    new FileOutputStream( downloadPath ) );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DATA_NOT_EXIST ) {
                throw e;
            }
        }
        actTestSuccessCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( actTestSuccessCount.get() == expTestSuccessCount
                    || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
            if ( session2 != null ) {
                session2.close();
            }
        }
    }

    private void createScmFile( ScmSession session, Date date )
            throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                session );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setCreateTime( date );
        file.setAuthor( name );
        file.setTitle( "sequoiacm" );
        file.setMimeType( "text/plain" );
        fileIdList.add( file.save() );
    }
}