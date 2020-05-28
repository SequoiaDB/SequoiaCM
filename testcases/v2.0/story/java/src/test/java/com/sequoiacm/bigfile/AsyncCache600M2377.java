package com.sequoiacm.bigfile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description: 异步缓存600M文件
 * @author fanyu
 * @Date:2019年02月14日
 * @version:1.0
 */
public class AsyncCache600M2377 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private long fileSize = 1024 * 1024 * 600;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "AsyncCache600M";
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();
        // login in
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
        sessionA = TestScmTools.createSession( branchSite );
        wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        writeFileFromMainCenter();
    }

    @Test(groups = { "fourSite" }) // SEQUOIACM-415
    private void test() throws Exception {
        ScmFactory.File.asyncCache( wsA, fileId );
        // check result
        SiteWrapper[] expSiteList = { rootSite, branchSite };
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, expSiteList.length );
        ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void writeFileFromMainCenter() throws ScmException {
        ScmFile scmfile = ScmFactory.File.createInstance( wsM );
        scmfile.setContent( filePath );
        scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
        fileId = scmfile.save();
        System.out
                .println( "writeFileFromMainCenter fileId = " + fileId.get() );
    }
}
