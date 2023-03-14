package com.sequoiacm.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
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
 * @Descreption SCM-3745:异步单文件缓存，待缓存文件在分站点
 * @Author YiPan
 * @CreateDate 2021/9/10
 * @Version 1.0
 */
public class AsyncCache3745 extends TestScmBase {
    private static final String fileName = "file3745";
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmSession branchSiteSession = null;
    private ScmWorkspace branchSiteWs = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();

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
        branchSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        branchSiteSession = ScmSessionUtils.createSession( branchSite );
        branchSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );

        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        // 分站点A创建文件
        fileId = ScmFileUtils.create( branchSiteWs, fileName, filePath );
        fileIds.add( fileId );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        // 主站点缓存
        SiteWrapper[] expSite = { branchSite, rootSite };
        ScmFactory.File.asyncCache( rootSiteWs, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId,
                expSite.length );
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSite );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( rootSiteWs, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            rootSiteSession.close();
            branchSiteSession.close();
        }
    }
}
