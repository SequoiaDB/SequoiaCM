package com.sequoiacm.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description: SCM-2417:目标站点存在残留文件，并发异步迁移和迁移
 * @author fanyu
 * @Date:2019年02月28日
 * @version:1.0
 */

public class TransferAndAsnycTransfer_WhenLobRemain2417 extends TestScmBase {
    private boolean runSuccess = false;
    private String authorName = "file2417";
    private int fileSize = 1024 * 1024;
    private ScmId taskId = null;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private ScmId randomId = null;
    private int fileNum = 5;
    private String remainFilePath = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        remainFilePath =
                localPath + File.separator + "localFile_" + fileSize / 2 +
                        ".2txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( remainFilePath, fileSize / 2 );

        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        session = TestScmTools.createSession( branchSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        writeFileFromSubCenterA();
        //make remain in rootsite and branchSite
        randomId = fileIdList.get( new Random().nextInt( fileIdList.size() ) );
        TestSdbTools.Lob.putLob( rootSite, wsp, randomId, remainFilePath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        StartTaskFromSubCenterA startTask = new StartTaskFromSubCenterA();
        startTask.start();
        AsnycTransferFileFromSubCenterB asnycTransferFile = new
                AsnycTransferFileFromSubCenterB();
        asnycTransferFile.start( 5 );
        Assert.assertEquals( startTask.isSuccess(), true,
                startTask.getErrorMsg() );
        Assert.assertEquals( asnycTransferFile.isSuccess(), true,
                asnycTransferFile.getErrorMsg() );
        //check result
        SiteWrapper[] expSiteList = { rootSite, branchSite };
        ScmTaskUtils.waitTaskFinish( session, taskId );
        for ( ScmId fileId : fileIdList ) {
            ScmTaskUtils
                    .waitAsyncTaskFinished( ws, fileId, expSiteList.length );
        }
        ScmFileUtils.checkMetaAndData( wsp, fileIdList, expSiteList, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void writeFileFromSubCenterA() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            fileIdList.add( file.save() );
        }
    }

    private class StartTaskFromSubCenterA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( branchSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // start task
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.SIZE )
                        .greaterThanEquals( fileSize )
                        .and( ScmAttributeName.File.AUTHOR ).is( authorName )
                        .get();
                taskId = ScmSystem.Task.startTransferTask( ws, condition );
            } finally {
                if ( session != null )
                    session.close();
            }
        }
    }

    private class AsnycTransferFileFromSubCenterB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession sessionA = null;
            try {
                sessionA = TestScmTools.createSession( branchSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionA );
                ScmFactory.File.asyncTransfer( ws, randomId );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }
}