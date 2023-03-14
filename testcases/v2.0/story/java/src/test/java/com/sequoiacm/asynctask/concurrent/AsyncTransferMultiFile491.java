package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
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
 * @Testcase: SCM-491:并发迁移多个不同文件
 * @author huangxiaoni init
 * @date 2017.6.26
 */

public class AsyncTransferMultiFile491 extends TestScmBase {
    private boolean runSuccess = false;

    private ScmSession sessionA = null; // subCenterA
    private ScmWorkspace wsA = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String author = "asyncTransfer491";
    private int fileSize = 1024 * new Random().nextInt( 2048 );
    private int fileNum = 50;
    private File localPath = null;
    private String filePath = null;

    private SiteWrapper rootSite = null, branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            ws_T = ScmInfo.getWs();
            rootSite = ScmInfo.getRootSite();
            branceSite = ScmScheduleUtils.getSortBranchSites().get( 0 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionA = ScmSessionUtils.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            writeFileFromA();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() {
        try {
            asyncTransferFromA();
            checkResult();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsA, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void asyncTransferFromA() throws Exception {
        for ( ScmId fileId : fileIdList ) {
            ScmFactory.File.asyncTransfer( wsA, fileId,
                    rootSite.getSiteName() );
        }
    }

    private void writeFileFromA() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setContent( filePath );
            file.setFileName( author + "_" + UUID.randomUUID() );
            file.setAuthor( author );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            for ( int i = 0; i < fileNum; i++ ) {
                ScmTaskUtils.waitAsyncTaskFinished( wsA, fileIdList.get( i ),
                        expSiteList.length );
            }
            ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}