package com.sequoiacm.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Descreption SCM-504:异步单文件缓存，待缓存文件在主站点
 * @Author fanyu
 * @CreateDate 2017-06-28
 * @UpdateUser YiPan
 * @UpdateDate 2021/9/8
 * @UpdateRemark 优化用例
 * @Version 1.0
 */
public class AsyncCache504 extends TestScmBase {
    private static final String fileName = "AsyncCache504";
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSiteA = null;
    private WsWrapper ws_T = null;
    private BSONObject queryCond;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branchSiteA = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();

        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
        sessionA = ScmSessionUtils.createSession( branchSiteA );
        wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( ws_T, queryCond );
        fileId = ScmFileUtils.create( wsM, fileName, filePath );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        ScmFactory.File.asyncCache( wsA, fileId );
        // check result
        SiteWrapper[] expSiteList = { rootSite, branchSiteA };
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, expSiteList.length );
        ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( ws_T, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            sessionM.close();
            sessionA.close();
        }
    }
}
