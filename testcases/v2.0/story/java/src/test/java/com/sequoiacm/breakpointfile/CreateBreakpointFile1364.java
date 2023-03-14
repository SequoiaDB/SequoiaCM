package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:create breakpoint file,upload empty data testlink
 * case:seqDB-1364
 * 
 * @author wuyan
 * @Date 2018.05.11
 * @version 1.00
 */

public class CreateBreakpointFile1364 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "breakpointfile1364";

    @BeforeClass
    private void setUp() throws ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        createEmptyBreakpointFile();
    }

    private void createEmptyBreakpointFile() throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );

        byte[] data = new byte[ 0 ];
        breakpointFile.upload( new ByteArrayInputStream( data ) );

        // check file's attribute
        // empty file is check the filesize is 0
        Assert.assertEquals( breakpointFile.getUploadSize(), 0 );
        Assert.assertEquals( breakpointFile.getWorkspace(), ws );
        Assert.assertEquals( breakpointFile.isCompleted(), true );

        // check the data of the file
        checkBreakPointData( data, breakpointFile );
    }

    private void checkBreakPointData( byte[] data,
            ScmBreakpointFile breakpointFile ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        fileId = file.save();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        byte[] fileData = outputStream.toByteArray();
        Assert.assertEquals( fileData, data );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}