package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.Assert;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @description SCM-1390:跨站点删除断点文件 SCM-3943:跨站点删除断点文件
 * @author wuyan
 * @createDate 2018.05.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class BreakpointFile1390_3943 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 2;
    private List< SiteWrapper > branSites = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private boolean runSuccess = false;
    private String fileName = "breakpointfile1390";
    private int fileSize = 1024 * 1024 * 5;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        DBSites.remove( 0 );
        if ( DBSites.size() < 2 ) {
            throw new SkipTestException( "need two DBSites, skip!" );
        }

        branSites = DBSites;
        wsp = ScmInfo.getWs();

        sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = ScmSessionUtils.createSession( branSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        createBreakpointFile();
        deleteBreakpointfile();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
                if ( sessionB != null ) {
                    sessionB.close();
                }
            }
        }
    }

    private void createBreakpointFile() throws ScmException, IOException {
        // create file
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( wsA, fileName, checksumType );
        breakpointFile.upload( new File( filePath ) );
    }

    private void deleteBreakpointfile() throws ScmException, IOException {

        // delete the breakpointfile by other site
        try {
            ScmFactory.BreakpointFile.deleteInstance( wsB, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() + e.getError() );
            }
        }

        // check the breakpointfile exist
        Assert.assertNotNull(
                ScmFactory.BreakpointFile.getInstance( wsA, fileName ),
                "file exist!" );
    }
}