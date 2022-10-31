package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-3937:断点文件未完成上传，设置为文件的内容 SCM-1379:断点文件未完成上传，设置为文件的内容
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3937_1379 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "file3937";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 5;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws ScmException, IOException {
        BreakpointUtil.createBreakpointFile( ws, filePath, fileName, 1024 * 512,
                ScmChecksumType.CRC32 );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );

        // 断点文件未上传完毕,转化为SCM文件
        breakpointFile2ScmFile( breakpointFile );
        // 检查断点文件
        checkBreakpointFile();
        // 检查SCM文件
        checkFile();
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

    private void breakpointFile2ScmFile( ScmBreakpointFile breakpointFile )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setTitle( fileName );
        fileId = file.save();
        file = ScmFactory.File.getInstance( ws, fileId );
        file.setContent( breakpointFile );
        try {
            file.save();
            Assert.fail( "unCompleted breakpointFile can't be file" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }
    }

    private void checkBreakpointFile() throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        Assert.assertEquals( breakpointFile.isCompleted(), false );
        Assert.assertEquals( breakpointFile.getUploadSize(), 1024 * 512 );
    }

    private void checkFile() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getSize(), 0 );
    }
}
