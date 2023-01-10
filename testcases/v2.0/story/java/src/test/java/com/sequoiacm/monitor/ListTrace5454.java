package com.sequoiacm.monitor;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.trace.ScmTrace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @descreption SCM-5454:ScmSystem.ServiceTrace驱动测试
 * @author YiPan
 * @date 2022/12/1
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ListTrace5454 extends TestScmBase {
    private ScmSession session = null;
    private String fileName = "file5454";
    private int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private ScmWorkspace ws;
    private WsWrapper wsp;
    private BSONObject query;
    private long now;
    private long hour;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
        hour = 60 * 60 * 1000L * 1000L;
        session = TestScmTools.createSession();
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        query = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );
        createFile();
    }

    @Test
    public void test() throws Exception {
        testLimit();

        testBeginAndEnd();

        testDuration();

        testQueryCondition();

        testGetTrace();
        runSuccess = true;
    }

    private void testGetTrace() throws ScmException {
        List< ScmTrace > traces = ScmSystem.ServiceTrace.listTrace( session,
                10 );
        for ( ScmTrace trace : traces ) {
            if ( trace.isComplete() ) {
                ScmTrace getScmTrace = ScmSystem.ServiceTrace.getTrace( session,
                        trace.getTraceId() );
                Assert.assertEquals( getScmTrace.toString(), trace.toString() );
                break;
            }
        }
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, query );
            }
        } finally {
            session.close();
        }
    }

    private void createFile() throws ScmException {
        for ( int i = 0; i < 10; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + i );
            file.setContent( filePath );
            file.setAuthor( fileName );
            file.save();
        }
    }

    private void testBeginAndEnd() throws ScmException {
        // test a: 范围内存在链路信息
        List< ScmTrace > scmTraces = ScmSystem.ServiceTrace.listTrace( session,
                new Date( now - hour ), new Date( now ), 1000 );
        Assert.assertNotEquals( scmTraces.size(), 0 );

        // test b: 范围内不存在链路信息
        scmTraces = ScmSystem.ServiceTrace.listTrace( session,
                new Date( now - hour * 72 ), new Date( now - hour * 48 ),
                1000 );
        Assert.assertEquals( scmTraces.size(), 0 );

        // test c: begin>end
        try {
            ScmSystem.ServiceTrace.listTrace( session, new Date( now + hour ),
                    new Date( now ), 1000 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
        }
    }

    private void testLimit() throws ScmException {
        // test a: limit<0
        try {
            ScmSystem.ServiceTrace.listTrace( session, -1 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
        }
        // test b: limit=0
        try {
            ScmSystem.ServiceTrace.listTrace( session, 0 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
        }
        // test c: limit>traces num
        List< ScmTrace > scmTraces = ScmSystem.ServiceTrace.listTrace( session,
                10000 );
        checkTraceNotNull( scmTraces );

        // test d: limit<traces num
        scmTraces = ScmSystem.ServiceTrace.listTrace( session,
                scmTraces.size() - 1 );
        checkTraceNotNull( scmTraces );
    }

    private void testDuration() throws ScmException {
        // test a： minDuration<0
        try {
            ScmSystem.ServiceTrace.listTrace( session, -1L, 1000 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
        }

        // test a： minDuration=0
        try {
            ScmSystem.ServiceTrace.listTrace( session, 0L, 1000 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
        }

        // test a： minDuration匹配不到
        List< ScmTrace > scmTraces = ScmSystem.ServiceTrace.listTrace( session,
                hour, 5 );
        Assert.assertEquals( scmTraces.size(), 0, scmTraces.toString() );

        // test a： minDuration匹配到
        scmTraces = ScmSystem.ServiceTrace.listTrace( session, 1L, 1000 );
        Assert.assertNotEquals( scmTraces.size(), 0 );
    }

    private void testQueryCondition() throws ScmException {
        HashMap< String, String > query = new HashMap<>();
        query.put( "http.url", "/v2/localLogin" );

        // test a：匹配到链路信息
        List< ScmTrace > scmTraces = ScmSystem.ServiceTrace.listTrace( session,
                null, 1L, new Date( now - hour ), new Date(), query, 100 );
        Assert.assertNotEquals( scmTraces.size(), 0 );
        for ( ScmTrace trace : scmTraces ) {
            Assert.assertTrue(
                    trace.getRequestUrl().contains( "/v2/localLogin" ) );
        }

        // test b：匹配不到链路信息
        query.put( "traceId", "12345" );
        scmTraces = ScmSystem.ServiceTrace.listTrace( session, null, 1L,
                new Date( now - hour ), new Date(), query, 100 );
        Assert.assertEquals( scmTraces.size(), 0 );
    }

    private void checkTraceNotNull( List< ScmTrace > scmTraces ) {
        for ( ScmTrace trace : scmTraces ) {
            if ( trace.isComplete() ) {
                Assert.assertNotNull( trace.getTraceId() );
                Assert.assertNotEquals( trace.getDuration(), 0 );
                Assert.assertNotNull( trace.getFirstSpan() );
                Assert.assertNotNull( trace.getRequestUrl() );
                Assert.assertNotEquals( trace.getSpanCount(), 0 );
            }
        }
    }
}
