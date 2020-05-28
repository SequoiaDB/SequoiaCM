package com.sequoiacm.scmfile.concurrent;

import java.io.File;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-140: 并发修改、查询同一文件信息
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、多线程并发，A线程修改同一个文件信息，B线程查询该文件信息 2、检查操作后的文件信息正确性
 */

public class UpdateWhileQuerying140 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int FILE_SIZE = 100;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private String fileName = "updateAttri140";
    private File localPath = null;
    private String filePath = null;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + FILE_SIZE
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, FILE_SIZE );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );
            fileId = ScmFileUtils.create( ws, fileName, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            UpdateThread updateThd = new UpdateThread();
            QueryThread queryThd = new QueryThread();

            updateThd.start();
            queryThd.start();

            if ( !( updateThd.isSuccess() && queryThd.isSuccess() ) ) {
                Assert.fail( updateThd.getErrorMsg() + queryThd.getErrorMsg() );
            }

            checkAttrUpdated();
            runSuccess = true;
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void checkAttrUpdated() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String str = fileName + "_" + 99;
            Assert.assertEquals( file.getAuthor(), str );
            Assert.assertEquals( file.getTitle(), str );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class QueryThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                int queryTimes = 1000;
                for ( int i = 0; i < queryTimes; ++i ) {
                    ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                    file.getAuthor();
                    file.getTitle();
                    file.getFileName();
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                int updateTimes = 100;
                for ( int i = 0; i < updateTimes; ++i ) {
                    String str = fileName + "_" + i;
                    file.setAuthor( str );
                    file.setTitle( str );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}