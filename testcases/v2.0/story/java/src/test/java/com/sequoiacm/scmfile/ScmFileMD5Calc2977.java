package com.sequoiacm.scmfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.UUID;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2977 :: 计算不存在的断点文件md5
 * @author fanyu
 * @Date:2020年8月27日
 * @version:1.0
 */
public class ScmFileMD5Calc2977 extends TestScmBase {
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2977" + "_" + UUID.randomUUID();
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
        ScmBreakpointFile breakpointFile = createBreakpointFile();
        ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        try {
            breakpointFile.calcMd5();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                throw e;
            }
        }
    }
    
    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        if ( session != null ) {
            session.close();
        }
    }

    private ScmBreakpointFile createBreakpointFile()
            throws ScmException, IOException {
        ScmBreakpointFileOption option = new ScmBreakpointFileOption();
        option.setNeedMd5( false );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, option );
        InputStream inputStream = new ByteArrayInputStream( bytes );
        breakpointFile.upload( inputStream );
        inputStream.close();
        return breakpointFile;
    }

}