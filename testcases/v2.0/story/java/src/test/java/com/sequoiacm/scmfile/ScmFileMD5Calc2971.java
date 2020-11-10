package com.sequoiacm.scmfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2971:指定计算md5,通过无md5的断点文件更新文件内容，计算断点文件md5,重新更新文件内容
 * @author fanyu
 * @Date:2020年8月26日
 * @version:1.0
 */
public class ScmFileMD5Calc2971 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2971" + "_" + UUID.randomUUID();
    private int fileSize = 200 * 1024;
    private ScmId fileId = null;
    private String filePath = null;
    private File localPath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // 创建无md5的文件
        createFile();

        // 创建无md5的断点文件
        ScmBreakpointFile breakpointFile = createBreakpointFile();

        // 指定计算md5,更新文件内容
        try {
            updateFile( breakpointFile );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // 计算断点文件md5
        breakpointFile.calcMd5();

        // 重新通过断点文件创建文件
        updateFile( breakpointFile );

        // 检查结果
        ScmFile currFile = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( currFile.getMd5(),
                TestTools.getMD5AsBase64( filePath ), fileId.get() );
        ScmFile histFile = ScmFactory.File.getInstance( ws, fileId, 1, 0 );
        Assert.assertNull( histFile.getMd5(), fileId.get() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( filePath );
        fileId = file.save();
    }

    private ScmBreakpointFile createBreakpointFile()
            throws ScmException, IOException {
        ScmBreakpointFileOption option = new ScmBreakpointFileOption();
        option.setNeedMd5( false );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, option );
        InputStream inputStream = new FileInputStream( filePath );
        breakpointFile.upload( inputStream );
        inputStream.close();
        return breakpointFile;
    }

    private void updateFile( ScmBreakpointFile breakpointFile )
            throws ScmException, FileNotFoundException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmUpdateContentOption option = new ScmUpdateContentOption();
        option.setNeedMd5( true );
        file.updateContent( breakpointFile, option );
    }

}