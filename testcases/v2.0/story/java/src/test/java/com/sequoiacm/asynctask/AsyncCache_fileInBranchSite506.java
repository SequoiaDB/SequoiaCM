package com.sequoiacm.asynctask;

import java.io.File;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-506: 分中心存在但主中心不存在
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在分中心A异步缓存单个文件； 2、检查执行结果正确性；
 */

public class AsyncCache_fileInBranchSite506 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AsyncCache_fileInBranchSite506.class );
    private final int fileSize = 200 * 1024;
    private boolean runSuccess = false;
    private ScmSession sessionA = null;
    private ScmId fileId = null;
    private String fileName = "AsyncCache506";
    private File localPath = null;
    private String filePath = null;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            sessionA = TestScmTools.createSession( branceSite );
            prepareFiles( sessionA );
        } catch ( Exception e ) {
            if ( sessionA != null ) {
                sessionA.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        try {
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), sessionA );
            System.out.println( "fileId = " + fileId.get() );
            ScmFactory.File.asyncCache( ws, fileId );
            Assert.fail(
                    "asyncCache shouldn't succeed when main site hasn't such " +
                            "file" );
        } catch ( ScmException e ) {
            if ( ScmError.DATA_NOT_EXIST != e.getError() ) {
                e.printStackTrace();
                logger.error( "fileId = " + fileId.get() );
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), sessionA );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void prepareFiles( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ws_T.getName(), session );
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( filePath );
        scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
        fileId = scmfile.save();
    }
}