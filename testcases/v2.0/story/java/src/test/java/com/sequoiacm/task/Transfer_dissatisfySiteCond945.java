/**
 *
 */
package com.sequoiacm.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Descreption SCM-945:迁移满足迁移条件但不满足站点条件的文件
 * @Author fanyu
 * @CreateDate 2017年10月30日
 * @UpdateUser YiPan
 * @UpdateDate 2021/9/14
 * @UpdateRemark 优化用例
 * @Version 1.0
 */
public class Transfer_dissatisfySiteCond945 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSites = null;
    private WsWrapper ws_T = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private BSONObject cond;

    private ScmId taskId = null;
    private final String authorName = "Transfer945";
    private int fileSize = 100;
    private int fileNum = 10;
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private List< ScmId > fileIdList3 = new ArrayList<>();
    private List< ScmId > fileIdList4 = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        // ready local file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        // get site and ws
        rootSite = ScmInfo.getRootSite();
        branceSites = ScmInfo.getBranchSites( 2 );
        ws_T = ScmInfo.getWs();

        // clean scmFile
        cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( ws_T, cond );

        // login
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );

        sessionA = TestScmTools.createSession( branceSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

        sessionB = TestScmTools.createSession( branceSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionB );

        // write scmFile
        writeFile( wsM, fileIdList1 );
        writeFile( wsA, fileIdList2 );
        writeFile( wsB, fileIdList3 );
        writeFile( wsA, fileIdList4 );

        // read scmFile
        readScmFile( wsM, fileIdList4 );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        taskId = ScmSystem.Task.startTransferTask( wsA, cond );
        ScmTaskUtils.waitTaskFinish( sessionA, taskId );

        checkResults();

        // check task info
        ScmTask taskInfo = ScmSystem.Task.getTask( sessionA, taskId );
        int totalFileCount = fileIdList1.size() + fileIdList2.size()
                + fileIdList3.size() + fileIdList4.size();
        int successFileCount = fileIdList2.size();
        Assert.assertEquals( taskInfo.getProgress(), 100 );
        Assert.assertEquals( taskInfo.getEstimateCount(), totalFileCount );
        Assert.assertEquals( taskInfo.getActualCount(), successFileCount );
        Assert.assertEquals( taskInfo.getFailCount(), 0 );
        Assert.assertEquals( taskInfo.getSuccessCount(), successFileCount );
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( ws_T, cond );
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void writeFile( ScmWorkspace ws, List< ScmId > fileIdList )
            throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void readScmFile( ScmWorkspace ws, List< ScmId > fileIdList )
            throws Exception {
        for ( int i = 0; i < fileIdList.size(); i++ ) {
            ScmId fileId = fileIdList.get( i );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
        }
    }

    private void checkResults() throws Exception {
        SiteWrapper[] expSiteArr1 = { rootSite };
        ScmFileUtils.checkMetaAndData( ws_T, fileIdList1, expSiteArr1,
                localPath, filePath );

        SiteWrapper[] expSiteArr2 = { rootSite, branceSites.get( 0 ) };
        ScmFileUtils.checkMetaAndData( ws_T, fileIdList2, expSiteArr2,
                localPath, filePath );

        SiteWrapper[] expSiteArr3 = { branceSites.get( 1 ) };
        ScmFileUtils.checkMetaAndData( ws_T, fileIdList3, expSiteArr3,
                localPath, filePath );

        SiteWrapper[] expSiteArr4 = { rootSite, branceSites.get( 0 ) };
        ScmFileUtils.checkMetaAndData( ws_T, fileIdList4, expSiteArr4,
                localPath, filePath );
    }
}
