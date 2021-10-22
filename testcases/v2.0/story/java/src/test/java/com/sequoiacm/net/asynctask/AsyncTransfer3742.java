package com.sequoiacm.net.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Descreption SCM-3742:异步单文件迁移，源站点为分站点
 * @Author YiPan
 * @CreateDate 2021/9/8
 * @Version 1.0
 */
public class AsyncTransfer3742 extends TestScmBase {
    private static final String fileName = "file3742";
    private boolean runSuccess = false;
    private List< ScmId > fileIds = new ArrayList<>();
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmSession branchSiteSession = null;
    private ScmWorkspace branchSiteWs = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite1 = null;
    private SiteWrapper branchSite2 = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private ScmId fileId;

    @BeforeClass
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > branchSites = ScmInfo.getBranchSites( 2 );
        branchSite1 = branchSites.get( 0 );
        branchSite2 = branchSites.get( 1 );
        wsp = ScmInfo.getWs();
        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        branchSiteSession = TestScmTools.createSession( branchSite1 );
        branchSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        fileId = ScmFileUtils.create( branchSiteWs, fileName, filePath );
        fileIds.add( fileId );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // 分站点迁移到主站点
        ScmFactory.File.asyncTransfer( branchSiteWs, fileId,
                rootSite.getSiteName() );
        SiteWrapper[] exp_2SiteList = { rootSite, branchSite1 };
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId,
                exp_2SiteList.length );
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, exp_2SiteList );

        // 分站点迁移到相同分站点
        try {
            ScmFactory.File.asyncTransfer( branchSiteWs, fileId,
                    branchSite1.getSiteName() );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }

        // 分站点迁移到其他分站点
        ScmFactory.File.asyncTransfer( branchSiteWs, fileId,
                branchSite2.getSiteName() );
        SiteWrapper[] exp_3SiteList = { rootSite, branchSite1, branchSite2 };
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId,
                exp_3SiteList.length );
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, exp_3SiteList );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            rootSiteSession.close();
            branchSiteSession.close();
        }
    }
}
