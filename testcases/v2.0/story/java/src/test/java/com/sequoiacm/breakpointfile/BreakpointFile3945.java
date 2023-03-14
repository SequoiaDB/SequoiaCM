package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;

/**
 * @description SCM-3945:指定ws和文件不存在，执行删除操作
 * @author zhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */

public class BreakpointFile3945 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "breakpointfile3945";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        byte[] data = new byte[ 1024 * 1024 * 5 ];
        new Random().nextBytes( data );
        breakpointFile.upload( new ByteArrayInputStream( data ) );

        ScmWorkspace noExistWs = null;
        String notExistBreakpointfileName = "notExistBreakpointfile3945";
        // ws不存在
        try {
            ScmFactory.BreakpointFile.deleteInstance( noExistWs, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        // filename不存在
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws,
                    notExistBreakpointfileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}