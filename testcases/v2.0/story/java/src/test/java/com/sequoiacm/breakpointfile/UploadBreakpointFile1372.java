package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:create empty breakpoint file, than continuation upload file
 * testlink case:seqDB-1372
 * 
 * @author wuyan
 * @Date 2018.05.18
 * @version 1.00
 */

public class UploadBreakpointFile1372 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmId fileId3 = null;
    private int fileSize = 1024 * 1024 * 1;
    private String fileName1 = "breakpointfile1372a";
    private String fileName2 = "breakpointfile1372b";
    private String fileName3 = "breakpointfile1372c";

    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
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
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        continuesUploadFileByCheckSum();
        continuesUploadFileByNoCheckSum();
        // http://jira:8080/browse/SEQUOIACM-266
        continuesUploadEmptyFile();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, fileId1, true );
            ScmFactory.File.deleteInstance( ws, fileId2, true );
            ScmFactory.File.deleteInstance( ws, fileId3, true );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    // continues upload file ,the file content is not empty
    private void continuesUploadFileByCheckSum() throws Exception {
        // continues upload file
        ScmChecksumType checksumType = ScmChecksumType.CRC32;
        createEmptyBreakpointFile( checksumType, fileName1 );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName1 );
        breakpointFile.upload( new File( filePath ) );
        fileId1 = checkUploadFileData( fileName1 );
    }

    private void continuesUploadFileByNoCheckSum() throws Exception {
        // continues upload file
        ScmChecksumType checksumType = ScmChecksumType.NONE;
        createEmptyBreakpointFile( checksumType, fileName2 );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName2 );
        breakpointFile.upload( new File( filePath ) );
        fileId2 = checkUploadFileData( fileName2 );

    }

    private void continuesUploadEmptyFile() throws Exception {
        // continues upload file
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        createEmptyBreakpointFile( checksumType, fileName3 );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName3 );
        byte[] data = new byte[ 0 ];
        breakpointFile.upload( new ByteArrayInputStream( data ) );
        fileId3 = checkUploadEmptyFileData( fileName3, data );
    }

    private ScmId checkUploadFileData( String fileName ) throws Exception {
        // save to file, than down file check the file data
        ScmFile file = ScmFactory.File.createInstance( ws );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        ScmId fileId = file.save();

        // down file
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        TestTools.LocalFile.removeFile( downloadPath );
        return fileId;
    }

    private ScmId checkUploadEmptyFileData( String fileName, byte[] expData )
            throws Exception {
        // save to file, than down file check the file data
        ScmFile file = ScmFactory.File.createInstance( ws );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        ScmId fileId = file.save();

        // down file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );

        // check results
        byte[] fileData = outputStream.toByteArray();
        Assert.assertEquals( TestTools.getMD5( fileData ),
                TestTools.getMD5( expData ) );
        return fileId;
    }

    private void createEmptyBreakpointFile( ScmChecksumType checksumType,
            String fileName ) throws ScmException, IOException {
        // create breakpointfile
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );
        byte[] data = new byte[ 0 ];
        breakpointFile.incrementalUpload( new ByteArrayInputStream( data ),
                false );
    }

}