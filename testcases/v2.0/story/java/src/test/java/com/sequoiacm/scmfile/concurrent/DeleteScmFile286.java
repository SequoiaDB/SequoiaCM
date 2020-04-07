package com.sequoiacm.scmfile.concurrent;

import java.io.File;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-286: 不同中心并发删除不同文件
 * @Author linsuqiang
 * @Date 2017-05-24
 * @Version 1.00
 */

/*
 * 1、不同中心并发删除不同文件，覆盖所有中心； 2、检查执行结果正确性； 3、检查该文件元数据、lob文件等是否均被删除；
 */

public class DeleteScmFile286 extends TestScmBase {
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

    private File localPath = null;
    private String filePath = null;
    private String fileName = "delete286";
    private String author = fileName;
    private int fileSize = 100;
    private int fileNum = 30; // on each site
    private List< ScmId > mainFileIds = new ArrayList<>();
    private List< ScmId > aSlvFileIds = new ArrayList<>();
    private List< ScmId > bSlvFileIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

            sessionA = TestScmTools.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            sessionB = TestScmTools.createSession( branSites.get( 1 ) );
            wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
            for ( int i = 0; i < fileNum; i++ ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR )
                        .is( author + "_M" + i ).get();
                BSONObject cond1 = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR )
                        .is( author + "_A" + i ).get();
                BSONObject cond2 = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR )
                        .is( author + "_B" + i ).get();
                ScmFileUtils.cleanFile( wsp, cond );
                ScmFileUtils.cleanFile( wsp, cond1 );
                ScmFileUtils.cleanFile( wsp, cond2 );
            }
            this.prepareScmFiles();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        ScmSession sessionB = null;
        try {
            DeleteFromM deleteFromM = new DeleteFromM();
            deleteFromM.start();

            DeleteFromA deleteFromA = new DeleteFromA();
            deleteFromA.start();

            DeleteFromB deleteFromB = new DeleteFromB();
            deleteFromB.start();

            if ( !( deleteFromM.isSuccess() && deleteFromA.isSuccess() &&
                    deleteFromB.isSuccess() ) ) {
                Assert.fail(
                        deleteFromM.getErrorMsg() + deleteFromA.getErrorMsg() +
                                deleteFromB.getErrorMsg() );
            }

            this.checkResults();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
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
            ScmId mFileId = ScmFileUtils
                    .create( wsM, fileName + "_M" + i, filePath );
            mainFileIds.add( mFileId );

            ScmId aFileId = ScmFileUtils
                    .create( wsA, fileName + "_A" + i, filePath );
            aSlvFileIds.add( aFileId );

            ScmId bFileId = ScmFileUtils
                    .create( wsB, fileName + "_B" + i, filePath );
            bSlvFileIds.add( bFileId );
        }
    }

    private void checkResults() throws Exception {
        try {
            // check data
            List< List< ScmId > > lists = new ArrayList<>();
            lists.add( mainFileIds );
            lists.add( aSlvFileIds );
            lists.add( bSlvFileIds );
            ScmWorkspace[] wsArr = { wsM, wsA, wsB };
            for ( int i = 0; i < lists.size(); i++ ) {
                for ( ScmId fileId : lists.get( i ) ) {
                    try {
                        // check meta
                        BSONObject cond = new BasicBSONObject(
                                ScmAttributeName.File.FILE_ID, fileId.get() );
                        long cnt = ScmFactory.File
                                .countInstance( wsM, ScopeType.SCOPE_CURRENT,
                                        cond );
                        Assert.assertEquals( cnt, 0 );
                        ScmFileUtils.checkData( wsArr[ i ], fileId, localPath,
                                filePath );
                        Assert.assertFalse( true,
                                "File is unExisted, except throw e, but success." );
                    } catch ( ScmException e ) {
                        Assert.assertEquals( e.getError(),
                                ScmError.FILE_NOT_FOUND, e.getMessage() );
                    }
                }
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private class DeleteFromM extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                for ( int i = 0; i < fileNum; i++ ) {
                    ScmFactory.File.getInstance( ws, mainFileIds.get( i ) )
                            .delete( true );
                }
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
                session = TestScmTools.createSession( branSites.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                for ( int i = 0; i < fileNum; i++ ) {
                    ScmFactory.File.getInstance( ws, aSlvFileIds.get( i ) )
                            .delete( true );
                }
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
                session = TestScmTools.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                for ( int i = 0; i < fileNum; i++ ) {
                    ScmFactory.File.getInstance( ws, bSlvFileIds.get( i ) )
                            .delete( true );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}