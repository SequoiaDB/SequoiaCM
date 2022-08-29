package com.sequoiacm.asynctask.serial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-756 : 并发异步迁移不同文件（大并发）
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、并发做如下操作，并发数如500并发（每个并发包含如下3个步骤）： 1）分中心A写入文件； 2）分中心A异步迁移该文件，检查文件元数据、文件内容正确性；
 * 3）删除文件该文件； 2、检查执行结果正确性；
 */

public class AsyncTransferDiffFile_highConcurrent756 extends TestScmBase {
    private final int threadNum = 10;
    private final String author = "asyncTransfer756";
    private final int fileSize = 20 * 1024;
    private final int fileNum = threadNum;
    private boolean runSuccess = false;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private ScmSession sessionA = null; // subCenterA
    private ScmWorkspace ws = null;
    private AtomicInteger fileNo = new AtomicInteger( 0 );
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        // ready localfile
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileNum; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSize + i + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize + i );
            filePathList.add( filePath );
        }

        rootSite = ScmInfo.getRootSite();
        branceSite = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        ScmFileUtils.cleanFile( ws_T, cond );

        // login
        sessionA = TestScmTools.createSession( branceSite );
        ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

        // ready scmfile
        writeFileFromA();
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException {
        AsyncTransferThread aThread = new AsyncTransferThread();
        aThread.start( threadNum );
        Assert.assertTrue( aThread.isSuccess(), aThread.getErrorMsg() );
        checkDeletion();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void writeFileFromA() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePathList.get( i ) );
            file.setFileName( author + "_" + UUID.randomUUID() );
            file.setAuthor( author );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void checkDeletion() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        long count = ScmFactory.File.countInstance( ws, ScopeType.SCOPE_CURRENT,
                cond );
        Assert.assertEquals( count, 0, "files are residual after deletion." );
    }

    private class AsyncTransferThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            ScmSession ss = null;
            try {
                ss = TestScmTools.createSession( branceSite );
                ScmWorkspace wsA = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), ss );
                int i = fileNo.getAndIncrement();
                synchronized ( fileIdList ) {
                    ScmId fileId = fileIdList.get( i );
                    ScmFactory.File.asyncTransfer( wsA, fileId,
                            rootSite.getSiteName() );
                    checkAsyncTransfer( fileId, i );
                    ScmFactory.File.getInstance( wsA, fileId ).delete( true );
                }

            } finally {
                if ( null != ss ) {
                    ss.close();
                }
            }
        }

        private void checkAsyncTransfer( ScmId fileId, int i )
                throws Exception {
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmTaskUtils.waitAsyncTaskFinished( ws, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePathList.get( i ) );
        }

    }

}