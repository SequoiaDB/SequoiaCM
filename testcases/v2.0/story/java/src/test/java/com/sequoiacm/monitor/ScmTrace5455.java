package com.sequoiacm.monitor;

import java.io.File;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.trace.ScmTrace;
import com.sequoiacm.client.element.trace.ScmTraceSpan;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @descreption SCM-5455:ScmTrace驱动测试
 * @author YiPan
 * @date 2022/12/1
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmTrace5455 extends TestScmBase {
    private ScmSession session = null;
    private String fileName = "file5455";
    private int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private ScmWorkspace ws;
    private WsWrapper wsp;
    private BSONObject query;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = ScmSessionUtils.createSession();
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        query = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );
        createFile();
    }

    @Test
    public void test() throws Exception {
        List< ScmTrace > scmTraces = ScmSystem.ServiceTrace.listTrace( session,
                10 );

        for ( ScmTrace trace : scmTraces ) {
            if ( trace.isComplete() ) {
                Assert.assertNotNull( trace.getTraceId() );
                Assert.assertNotEquals( trace.getDuration(), 0 );
                Assert.assertNotNull( trace.getRequestUrl() );
                int expCount = trace.getSpanCount();
                int actCount = CountSpan( trace );
                Assert.assertEquals( actCount, expCount );
            }
        }

        runSuccess = true;
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
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( filePath );
        file.save();
    }

    /**
     * @Descreption 递归遍历所有span
     * @param scmTrace
     * @return
     */
    private int CountSpan( ScmTrace scmTrace ) {
        ScmTraceSpan firstSpan = scmTrace.getFirstSpan();
        return GetSpan( firstSpan ) + 1;
    }

    private int GetSpan( ScmTraceSpan firstSpan ) {
        int count = 0;
        List< ScmTraceSpan > nextSpans = firstSpan.getNextSpans();
        if ( nextSpans.size() != 0 ) {
            count = count + nextSpans.size();
            for ( ScmTraceSpan span : nextSpans ) {
                count = count + GetSpan( span );
            }
        } else {
            return count;
        }
        return count;
    }
}
