package com.sequoiacm.scmfile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
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
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2978 ::不指定计算md5,创建文件/创建断点文件/更新文件内容
 * @author fanyu
 * @Date:2020年8月27日
 * @version:1.0
 */
public class ScmFileMD5Calc2978 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2977" + "_" + UUID.randomUUID();
    private ScmId fileId = null;
    private byte[] bytes = new byte[ 1 ];

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        new Random().nextBytes( bytes );
    }

    @Test
    private void test() throws Exception {
        createBreakpointFile();
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        Assert.assertNull( breakpointFile.getMd5(),
                breakpointFile.getDataId() );

        createFile();
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertNull( file1.getMd5(), fileId.get() );

        updateFile();
        ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertNull( file2.getMd5(), fileId.get() );
        runSuccess = true;
    }
    
    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createBreakpointFile() throws ScmException, IOException {
        ScmBreakpointFileOption option = new ScmBreakpointFileOption();
        option.setNeedMd5( false );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, option );
        InputStream inputStream = new ByteArrayInputStream( bytes );
        breakpointFile.upload( inputStream );
        inputStream.close();
    }

    private void createFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        ScmUploadConf scmUploadConf = new ScmUploadConf( true, false );
        file.setFileName( fileName );
        file.setContent( new ByteArrayInputStream( bytes ) );
        fileId = file.save( scmUploadConf );
    }

    private void updateFile() throws ScmException, FileNotFoundException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmUpdateContentOption option = new ScmUpdateContentOption();
        option.setNeedMd5( false );
        file.updateContent( new ByteArrayInputStream( bytes ), option );
    }
}