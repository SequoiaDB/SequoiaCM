package com.sequoiacm.scmfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2975 :: 计算已有断点文件md5，断点文件无md5且在本地
 * @author fanyu
 * @Date:2020年8月27日
 * @version:1.0
 */
public class ScmFileMD5Calc2975 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2975" + "_" + UUID.randomUUID();
    private byte[] bytes = new byte[1024*200];

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        new Random(  ).nextBytes( bytes );
    }

    @Test
    private void test() throws Exception {
        // 创建无md5的断点文件
        ScmBreakpointFile breakpointFile = createBreakpointFile();
        Assert.assertFalse( breakpointFile.isNeedMd5() );

        // 通过断点文件创建文件
        breakpointFile.calcMd5();
        Assert.assertTrue( breakpointFile.isNeedMd5() );

        // 检查结果
        Assert.assertEquals( breakpointFile.getMd5(),
                TestTools.getMD5AsBase64( new ByteArrayInputStream( bytes ) ) );
        runSuccess = true;
    }
    
    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmBreakpointFile createBreakpointFile()
            throws ScmException, IOException {
        ScmBreakpointFileOption option = new ScmBreakpointFileOption();
        option.setNeedMd5( false);
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, option );
        InputStream inputStream = new ByteArrayInputStream( bytes );
        breakpointFile.upload( inputStream );
        inputStream.close();
        return breakpointFile;
    }
}