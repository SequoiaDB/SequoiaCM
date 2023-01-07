package com.sequoiacm.monitor;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.trace.ScmTrace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @descreption SCM-5453:通过驱动列取链路信息和外部操作并发
 * @author YiPan
 * @date 2022/12/1
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ListTrace5453 extends TestScmBase {
    private ScmSession session = null;
    private String fileName = "file5453";
    private int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private ScmWorkspace ws;
    private ScmId fileID;
    private WsWrapper wsp;
    private SiteWrapper site;
    private BSONObject cond;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        cond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new ListTrace() );
        t.addWorker( new CreatFile() );
        t.run();
        SiteWrapper[] sites = { site };
        ScmFileUtils.checkMetaAndData( wsp, fileID, sites, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, cond );
            }
        } finally {
            session.close();
        }
    }

    private class ListTrace {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = TestScmTools.createSession();
            try {
                List< ScmTrace > scmTraces = ScmSystem.ServiceTrace
                        .listTrace( session, 10 );
                Assert.assertNotEquals( scmTraces.size(), 0 );
                for ( ScmTrace trace : scmTraces ) {
                    if ( trace.isComplete() ) {
                        Assert.assertNotNull( trace.getTraceId() );
                        Assert.assertNotEquals( trace.getDuration(), 0 );
                        Assert.assertNotNull( trace.getFirstSpan() );
                        Assert.assertNotNull( trace.getRequestUrl() );
                        Assert.assertNotEquals( trace.getSpanCount(), 0 );
                    }
                }
            } finally {
                session.close();
            }
        }
    }

    private class CreatFile {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName );
                file.setContent( filePath );
                fileID = file.save();
            } finally {
                session.close();
            }
        }
    }
}
