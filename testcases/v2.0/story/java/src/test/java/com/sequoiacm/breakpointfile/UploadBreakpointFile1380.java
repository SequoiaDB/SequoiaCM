package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
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
 * test content:mulitple upload an breakpoint file by file
 * testlink-case:SCM-1380
 * 
 * @author wuyan
 * @Date 2018.05.22
 * @version 1.00
 */

public class UploadBreakpointFile1380 extends TestScmBase {
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private static SiteWrapper site = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    private String fileName = "breakpointfile1380";
    private int fileSize = 1024 * 1024 * 5;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        ScmBreakpointFile breakpointFile = uploadBreakpointFile();
        checkFileData( breakpointFile );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmBreakpointFile uploadBreakpointFile()
            throws ScmException, IOException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, ScmChecksumType.ADLER32 );

        // multiple upload file
        int[] dataSizes = { 1024 * 50, 1024 * 1024, 1024 * 1024 * 2 };
        for ( int i = 0; i < 3; i++ ) {
            InputStream inputStream = new BreakpointStream( filePath,
                    dataSizes[ i ] );
            if ( i != 0 ) {
                inputStream.read( new byte[ dataSizes[ i ] ], 0,
                        dataSizes[ i ] );
            }
            breakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();
        }

        // upload over
        breakpointFile.upload( new File( filePath ) );

        // check file's attribute
        Assert.assertEquals( breakpointFile.getUploadSize(), fileSize );
        Assert.assertEquals( breakpointFile.getWorkspace(), ws );
        Assert.assertEquals( breakpointFile.isCompleted(), true );
        return breakpointFile;
    }

    private void checkFileData( ScmBreakpointFile breakpointFile )
            throws Exception {
        // save to file, than down file check the file data
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        fileId = file.save();

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