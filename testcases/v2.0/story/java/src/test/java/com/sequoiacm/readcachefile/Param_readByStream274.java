package com.sequoiacm.readcachefile;

import java.io.IOException;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase:SCM-274 read输出流方式，参数校验 （A/B网络不通） read(OutputStream out)
 *                   有效参数：输出流路径存在且正确(基本功能覆盖)； 无效参数：null
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 */

public class Param_readByStream274 extends TestScmBase {
    private boolean runSuccess = false;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private ScmId fileId = null;
    private String fileName = "scmfile274";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException {
        try {
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession();
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            // write file
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            fileId = file.save();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite", "star" })
    private void testPathIsNull() throws ScmException {
        ScmInputStream sis = null;
        try {
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            sis = ScmFactory.File.createInputStream( scmfile );
            sis.read( null );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( sis != null )
                sis.close();
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }
}
