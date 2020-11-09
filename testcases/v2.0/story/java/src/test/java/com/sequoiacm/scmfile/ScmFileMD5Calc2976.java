package com.sequoiacm.scmfile;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @Description: SCM-2976 :: 计算已有断点文件md5,断点文件未完成或已有md5或在远程
 * @author fanyu
 * @Date:2020年8月27日
 * @version:1.0
 */
public class ScmFileMD5Calc2976 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site1 = null;
    private SiteWrapper site2 = null;
    private WsWrapper wsp = null;
    private ScmSession session1 = null;
    private ScmSession session2 = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;
    private String fileName = "file2976" + "_" + UUID.randomUUID();
    private byte[] bytes = new byte[ 1024 * 200 ];

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        site1 = sites.get( 0 );
        site2 = sites.get( 1 );
        wsp = ScmInfo.getWs();
        session1 = TestScmTools.createSession( site1 );
        session2 = TestScmTools.createSession( site2 );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session2 );
        new Random().nextBytes( bytes );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        String expMd5 = TestTools
                .getMD5AsBase64( new ByteArrayInputStream( bytes ) );
        // 计算md5,文件在本地且有md5
        ScmBreakpointFile breakpointFile1 = createBreakpointFile();
        breakpointFile1.calcMd5();
        Assert.assertEquals( breakpointFile1.getMd5(), expMd5 );

        // 计算md5，文件在远程且有md5
        ScmBreakpointFile breakpointFile2 = ScmFactory.BreakpointFile
                .getInstance( ws2, fileName );
        breakpointFile2.calcMd5();
        Assert.assertEquals( breakpointFile2.getMd5(), expMd5 );

        // 计算md5,断点文件未完成
        ScmFactory.BreakpointFile.deleteInstance( ws1, fileName );
        ScmBreakpointFile breakpointFile3 = ScmFactory.BreakpointFile
                .createInstance( ws1, fileName, ScmChecksumType.ADLER32 );
        breakpointFile3.incrementalUpload( new ByteArrayInputStream( bytes ),
                false );
        ScmBreakpointFile breakpointFile4 = ScmFactory.BreakpointFile
                .getInstance( ws1, fileName );
        try {
            breakpointFile4.calcMd5();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        runSuccess = true;
    }
    
    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.BreakpointFile.deleteInstance( ws1, fileName );
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

    private ScmBreakpointFile createBreakpointFile()
            throws ScmException, IOException {
        ScmBreakpointFileOption option = new ScmBreakpointFileOption();
        option.setNeedMd5( true );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws1, fileName, option );
        InputStream inputStream = new ByteArrayInputStream( bytes );
        breakpointFile.upload( inputStream );
        inputStream.close();
        return breakpointFile;
    }
}