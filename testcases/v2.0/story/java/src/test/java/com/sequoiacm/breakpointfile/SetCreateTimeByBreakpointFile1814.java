package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:set createTime when then setContent to file
 * testlink-case:SCM-1814
 *
 * @author wuyan
 * @Date 2018.06.21
 * @version 1.00
 */

public class SetCreateTimeByBreakpointFile1814 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private byte[] filedata1 = new byte[ 1024 * 100 ];
    private byte[] filedata2 = new byte[ 1024 * 20 ];
    private byte[] filedata3 = new byte[ 1024 * 50 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        long currentTimestamp = new Date().getTime();

        // test a: the setCreateTime interval within one month
        long timestamp1 = currentTimestamp - 10000;
        String fileName1 = "file1814a";
        createBreakpointFile( ws, fileName1, filedata1, timestamp1 );
        setContentFileAndcheckResult( fileName1, filedata1, timestamp1 );

        // test b :at least 31 days between different months,the timestamp is
        // 2678400000ms
        long timestamp2 = currentTimestamp - 2678400000l;
        String fileName2 = "file1814b";
        createBreakpointFile( ws, fileName2, filedata2, timestamp2 );
        setContentFileAndcheckResult( fileName2, filedata2, timestamp2 );

        // test c :not the same year at least 365 days,,the timestamp is
        // 31536000000ms
        long timestamp3 = currentTimestamp - 51536000000l;
        String fileName3 = "file1814c";
        createBreakpointFile( ws, fileName3, filedata3, timestamp3 );
        setContentFileAndcheckResult( fileName3, filedata3, timestamp3 );
    }

    @AfterClass
    private void tearDown() {
        try {

        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void createBreakpointFile( ScmWorkspace ws, String fileName,
            byte[] data, long timestamp ) throws ScmException {
        ScmChecksumType checksumType = ScmChecksumType.CRC32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );
        breakpointFile.upload( new ByteArrayInputStream( data ) );
    }

    private void setContentFileAndcheckResult( String fileName, byte[] expData,
            long timestamp ) throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        // setContent to file,and setCreateTime
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        Date date = new Date( timestamp );
        file.setCreateTime( date );
        ScmId fileId = file.save();

        Date actCreateTime = file.getCreateTime();
        Assert.assertEquals( actCreateTime, date );

        // download file and check file data
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        byte[] fileData = outputStream.toByteArray();
        Assert.assertEquals( fileData, expData );

        // delete the file
        ScmFactory.File.deleteInstance( ws, fileId, true );
    }

}