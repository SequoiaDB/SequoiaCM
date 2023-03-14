package com.sequoiacm.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
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
 * @Descreption SCM-485:异步单文件迁移，源站点为主站点
 * @Author linsuqiang
 * @CreateDate 2017-06-23
 * @UpdateUser YiPan
 * @UpdateDate 2021/9/8
 * @UpdateRemark 优化用例
 * @Version 1.00
 */

public class AsyncTransfer485 extends TestScmBase {
    private static final String fileName = "file485";
    private boolean runSuccess = false;
    private List< ScmId > fileIds = new ArrayList<>();
    private final int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private ScmId fileId;

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
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        fileId = ScmFileUtils.create( rootSiteWs, fileName, filePath );
        fileIds.add( fileId );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        // 主站点迁移到分站点
        ScmFactory.File.asyncTransfer( rootSiteWs, fileId,
                branchSite.getSiteName() );
        SiteWrapper[] expSiteList = { rootSite, branchSite };
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId,
                expSiteList.length );
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSiteList );

        // 主站点迁移到主站点
        try {
            ScmFactory.File.asyncTransfer( rootSiteWs, fileId,
                    rootSite.getSiteName() );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            rootSiteSession.close();
        }
    }
}