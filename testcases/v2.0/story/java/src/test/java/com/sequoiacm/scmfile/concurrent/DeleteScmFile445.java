package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-445: 并发删除不同文件
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、多线程并发删除不同文件（调用ScmFactory.File.delete接口） 2、检查删除结果；
 */

public class DeleteScmFile445 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private String fileName = "delete445";
    private int fileSize = 1024 * 1024 * 2;
    private int fileNum = 3;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

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

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            sessionM = ScmSessionUtils.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

            sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            sessionB = ScmSessionUtils.createSession( branSites.get( 1 ) );
            wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );

            for ( int i = 0; i < fileNum; i++ ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME )
                        .is( fileName + "_" + i ).get();
                ScmFileUtils.cleanFile( wsp, cond );
            }
            this.prepareScmFiles();
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            DeleteFromM deleteFromM = new DeleteFromM();
            deleteFromM.start();

            DeleteFromA deleteFromA = new DeleteFromA();
            deleteFromA.start();

            DeleteFromB deleteFromB = new DeleteFromB();
            deleteFromB.start();

            if ( !( deleteFromM.isSuccess() && deleteFromA.isSuccess()
                    && deleteFromB.isSuccess() ) ) {
                Assert.fail(
                        deleteFromM.getErrorMsg() + deleteFromA.getErrorMsg()
                                + deleteFromB.getErrorMsg() );
            }

            checkResults();

        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionM != null )
                sessionM.close();
            if ( sessionA != null )
                sessionA.close();
            if ( sessionB != null )
                sessionB.close();

        }
    }

    private void prepareScmFiles() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( wsA, fileName + "_" + i,
                    filePath );
            fileIdList.add( fileId );
        }
    }

    private void checkResults() throws Exception {
        for ( ScmId fileId : fileIdList ) {
            ScmWorkspace[] wsArr = { wsM, wsA, wsB };
            for ( int j = 0; j < wsArr.length; j++ ) {
                try {
                    // check meta
                    BSONObject cond = new BasicBSONObject(
                            ScmAttributeName.File.FILE_ID, fileId.get() );
                    long cnt = ScmFactory.File.countInstance( wsM,
                            ScopeType.SCOPE_CURRENT, cond );
                    Assert.assertEquals( cnt, 0 );
                    ScmFileUtils.checkData( wsArr[ j ], fileId, localPath,
                            filePath );
                    Assert.assertFalse( true, "File is unExisted, "
                            + "except throw e, but success." );
                } catch ( ScmException e ) {
                    Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                            e.getMessage() );
                }
            }
        }
    }

    private class DeleteFromM extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // delete
                ScmFactory.File.deleteInstance( ws, fileIdList.get( 0 ), true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFromA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branSites.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // delete
                ScmFactory.File.deleteInstance( ws, fileIdList.get( 1 ), true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFromB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // delete
                ScmFactory.File.deleteInstance( ws, fileIdList.get( 2 ), true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
