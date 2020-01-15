package com.sequoiacm.directory;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2595 :: 分页 directory.listFiles参数校验
 * @author fanyu
 * @Date:2019年09月04日
 * @version:1.0
 */
public class ListFileInPaDir_Param2595 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String dirPath = "/dir2595";
    private ScmDirectory scmDirectory;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        //clean
        if ( ScmFactory.Directory.isInstanceExist( ws, dirPath ) ) {
            ScmFactory.Directory.deleteInstance( ws, dirPath );
        }
        scmDirectory = ScmFactory.Directory.createInstance( ws, dirPath );
    }

    @Test
    private void testInvalidlimit() throws ScmException {
        try {
            scmDirectory.listFiles( new BasicBSONObject(), 0, -2,
                    new BasicBSONObject() );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void testInvalidSkip() throws ScmException {
        try {
            scmDirectory.listFiles( new BasicBSONObject(), -1, 1,
                    new BasicBSONObject() );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            ScmFactory.Directory.deleteInstance( ws, dirPath );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}


