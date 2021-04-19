/**
 *
 */
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
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1377_1386.java, 跨站点断点续传文件,跨站点获取断点文件信息
 * @author luweikang
 * @date 2018年5月21日
 */
public class BreakpointFile1377_1386 extends TestScmBase {

    private static List< SiteWrapper > siteList = null;
    private static WsWrapper wsp = null;
    private static ScmSession session1 = null;
    private static ScmSession session2 = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;

    private String fileName = "scmfile1377";
    private int fileSize = 1024 * 1024 * 5;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        siteList = ScmInfo.getAllSites();
        wsp = ScmInfo.getWs();
        session1 = TestScmTools.createSession( siteList.get( 0 ) );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException, IOException {

        BreakpointUtil.createBreakpointFile( ws1, filePath, fileName,
                1024 * 512, ScmChecksumType.CRC32 );
        // 跨站点上传断点文件
        this.uploadBreakpointFile();
        // 跨站点查询断点文件信息
        this.checkBreakpointFile();

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws1, fileName );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
            if ( session2 != null ) {
                session2.close();
            }

        }
    }

    private void uploadBreakpointFile() throws ScmException, IOException {

        session2 = TestScmTools.createSession( siteList.get( 1 ) );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session2 );

        InputStream inputStream = new FileInputStream( filePath );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws2, fileName );
        try {
            breakpointFile.upload( inputStream );
            Assert.fail( "different site upload breakpointFile should fail" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.INVALID_ARGUMENT.getErrorCode(),
                    "different site upload breakpointFile" );
        }
        inputStream.close();
    }

    private void checkBreakpointFile() throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws2, fileName );
        Assert.assertEquals( breakpointFile.isCompleted(), false,
                "check breakpointFile isCompleted" );
        Assert.assertEquals( breakpointFile.getSiteName(),
                siteList.get( 0 ).getSiteName(), "check breakpointFile site" );
        Assert.assertEquals( breakpointFile.getUploadSize(), 1024 * 512,
                "check breakpointFile uploadSize" );
    }

}
