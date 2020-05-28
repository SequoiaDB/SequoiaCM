package com.sequoiacm.task.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
import com.sequoiacm.client.core.ScmInputStream;
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
 * @Description: SCM-2403 ::目标站点存在残留文件，并发迁移和跨中心读
 * @author fanyu
 * @Date:2019年02月28日
 * @version:1.0
 */

public class TransferAndReadCacheFile_WhenLobRemain2403 extends TestScmBase {
    private boolean runSuccess = false;
    private String authorName = "file2403";
    private int fileSize = 1024 * 1024;
    private ScmId taskId = null;
    private File localPath = null;
    private String filePath = null;
    private String remainFilePath = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = null;
    private ScmId fileId = null;
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        remainFilePath = localPath + File.separator + "localFile_"
                + fileSize / 2 + ".2txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( remainFilePath, fileSize / 2 );
        rootSite = ScmInfo.getRootSite();
        branceSiteList = ScmInfo.getBranchSites( 2 );
        wsp = ScmInfo.getWs();
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        session = TestScmTools.createSession( branceSiteList.get( 0 ) );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        writeFileFromSubCenterA();
        // make remain in rootsite and branchSite
        TestSdbTools.Lob.putLob( rootSite, wsp, fileId, remainFilePath );
        TestSdbTools.Lob.putLob( branceSiteList.get( 1 ), wsp, fileId,
                filePath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        StartTaskFromSubCenterA startTask = new StartTaskFromSubCenterA();
        startTask.start();
        ReadFileFromSubCenterB readFile = new ReadFileFromSubCenterB();
        readFile.start( 5 );
        Assert.assertEquals( startTask.isSuccess(), true,
                startTask.getErrorMsg() );
        Assert.assertEquals( readFile.isSuccess(), true,
                readFile.getErrorMsg() );
        ScmTaskUtils.waitTaskFinish( session, taskId );
        SiteWrapper[] expSiteList = { rootSite, branceSiteList.get( 0 ),
                branceSiteList.get( 1 ) };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
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
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( authorName + "_" + UUID.randomUUID() );
        file.setAuthor( authorName );
        fileId = file.save();
    }

    private class StartTaskFromSubCenterA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( branceSiteList.get( 0 ) );
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

    private class ReadFileFromSubCenterB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            OutputStream fos = null;
            ScmInputStream sis = null;
            try {
                // login
                session = TestScmTools.createSession( branceSiteList.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( file );
                sis.read( fos );
            } finally {
                if ( fos != null )
                    fos.close();
                if ( sis != null )
                    sis.close();
                if ( session != null )
                    session.close();
            }
        }
    }
}