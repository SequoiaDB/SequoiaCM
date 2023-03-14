package com.sequoiacm.version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1643:断点文件未完成上传，更新为当前文件内容
 * @author wuyan
 * @createDate 2018.06.01
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateContentByBreakPointFile1643 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private boolean runSuccess = false;
    private String fileName = "file1643";
    private int breakpointfileSize = 1024 * 1024 * 3;
    private byte[] filedata = new byte[ 1024 * 500 ];
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        VersionUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_"
                + breakpointfileSize + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, breakpointfileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test
    private void test() throws Exception {
        fileId = ScmFileUtils.createFileByStream( ws, fileName, filedata );
        updateContentByBreakPointFile();

        int currentVersion = 1;
        VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                filedata );
        VersionUtils.checkFileCurrentVersion( ws, fileId, currentVersion );
        checkBreakPointFile();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void updateContentByBreakPointFile()
            throws ScmException, IOException {
        // create breakpointfile
        createBreakPointFile();
        // updataContent of file
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        try {
            file.updateContent( breakpointFile );
            Assert.fail(
                    "updateContent by  not uploaded breakpoint file must be "
                            + "fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                Assert.fail( "expErrorCode:-101  actError:" + e.getError()
                        + e.getMessage() );
            }
        }

    }

    private void createBreakPointFile() throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        int uploadSize = 1024 * 51;
        InputStream inputStream = new BreakpointStream( filePath, uploadSize );
        breakpointFile.incrementalUpload( inputStream, false );
        inputStream.close();
    }

    private void checkBreakPointFile() throws ScmException {
        // check breakpointfile is exist
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        Assert.assertEquals( breakpointFile.getFileName(), fileName );
    }

    class BreakpointStream extends InputStream {

        private FileInputStream in = null;
        private int finishByteNum = 0;
        private int breakNum;

        public BreakpointStream( String filePath, int breakNum )
                throws FileNotFoundException {
            this.in = new FileInputStream( filePath );
            this.breakNum = breakNum;
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public int read() throws IOException {
            int rs = in.read();
            if ( finishByteNum >= breakNum ) {
                rs = -1;
            }
            finishByteNum++;
            return rs;
        }
    }

}