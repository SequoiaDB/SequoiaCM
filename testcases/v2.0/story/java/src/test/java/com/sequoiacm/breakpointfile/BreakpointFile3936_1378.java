package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-3936:设置断点文件为文件的内容，执行文件操作 SCM-1378:设置断点文件为文件的内容，执行文件操作
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3936_1378 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 2;
    private List< SiteWrapper > branSites = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private ScmId fileId = null;

    private String fileName = "file1378";
    private int fileSize = 1024 * 1024 * 55;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

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

        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( branSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        createBreakpointFile();
        uploadAndSetContentFile();
        downLoadFileAndCheckData();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsB, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void createBreakpointFile() throws ScmException, IOException {
        // create breakpointfile
        ScmChecksumType checksumType = ScmChecksumType.CRC32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( wsA, fileName, checksumType );
        int uploadSize = 1024 * 51;
        InputStream inputStream = new BreakpointStream( filePath, uploadSize );
        breakpointFile.incrementalUpload( inputStream, false );
        inputStream.close();
    }

    private void uploadAndSetContentFile() throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( wsA, fileName );
        FileInputStream fStream = new FileInputStream( filePath );
        breakpointFile.upload( fStream );

        ScmFile file = ScmFactory.File.createInstance( wsA );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        fileId = file.save();

        // check breakpointfile not exist
        try {
            ScmFactory.BreakpointFile.getInstance( wsA, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }
    }

    private void downLoadFileAndCheckData() throws Exception {
        ScmFile file = ScmFactory.File.getInstanceByPath( wsB, fileName );
        // down file
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );
        TestTools.LocalFile.removeFile( downloadPath );
    }
}