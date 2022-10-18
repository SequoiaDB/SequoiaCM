/**
 *
 */
package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.SkipException;
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
 * @descreption SCM-3935:跨站点断点续传文件 SCM-1377:跨站点断点续传文件 SCM-1386:跨站点获取断点文件信息
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3935_1377_1386 extends TestScmBase {

    private static List< SiteWrapper > siteList = null;
    private static WsWrapper wsp = null;
    private static ScmSession session1 = null;
    private static ScmSession session2 = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;

    private String fileName = "file3935";
    private int fileSize = 1024 * 1024 * 5;
    private int uploadSize = 1024 * 512;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass()
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        siteList = ScmBreakpointFileUtils.checkDBDataSource();
        if ( siteList.size() < 2 ) {
            throw new SkipException( "指定类型站点数量不足！" );
        }
        wsp = ScmInfo.getWs();
        session1 = TestScmTools.createSession( siteList.get( 0 ) );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );

        session2 = TestScmTools.createSession( siteList.get( 1 ) );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session2 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException, IOException {
        // ws1上传文件
        BreakpointUtil.createBreakpointFile( ws1, filePath, fileName,
                uploadSize, ScmChecksumType.CRC32 );
        // ws2跨站点上传断点文件
        uploadBreakpointFile( ws2 );

        // ws2跨站点查询断点文件信息
        checkBreakpointFile( ws2, siteList.get( 0 ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.BreakpointFile.deleteInstance( ws1, fileName );
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

    private void uploadBreakpointFile( ScmWorkspace ws )
            throws ScmException, IOException {
        InputStream inputStream = new FileInputStream( filePath );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        try {
            breakpointFile.upload( inputStream );
            Assert.fail( "different site upload breakpointFile should fail" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        inputStream.close();
    }

    private void checkBreakpointFile( ScmWorkspace ws, SiteWrapper site )
            throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        Assert.assertEquals( breakpointFile.isCompleted(), false );
        Assert.assertEquals( breakpointFile.getSiteName(), site.getSiteName() );
        Assert.assertEquals( breakpointFile.getUploadSize(), uploadSize );
    }

}
